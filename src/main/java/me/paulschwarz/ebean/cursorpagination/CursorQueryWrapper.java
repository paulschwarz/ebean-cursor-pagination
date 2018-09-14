package me.paulschwarz.ebean.cursorpagination;

import static me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding.decodeCursor;
import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.order;

import io.ebean.ExpressionList;
import io.ebean.OrderBy.Property;
import io.ebean.Query;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.querydefn.DefaultOrmQuery;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding.Cursor;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.OrderBuilder;
import me.paulschwarz.ebean.cursorpagination.exceptions.BadCursorArgumentException;
import me.paulschwarz.ebean.cursorpagination.exceptions.CursedQueryException;
import me.paulschwarz.ebean.cursorpagination.exceptions.InvalidBeanException;
import me.paulschwarz.ebean.cursorpagination.exceptions.InvalidNaturalOrderingException;
import me.paulschwarz.ebean.cursorpagination.exceptions.MissingBaseQueryException;
import me.paulschwarz.ebean.cursorpagination.exceptions.MissingConverterException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CursorQueryWrapper<T> {

  private Logger logger = LoggerFactory.getLogger(CursorQueryWrapper.class);

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static class Builder<T> {

    private Map<Class, Function<String, Object>> converters;
    private Query<T> baseQuery;
    private OrderBuilder naturalOrdering;

    private Builder() {
      converters = new HashMap<>();
      converters.put(Integer.class, Integer::parseInt);
      converters.put(String.class, String::toString);
      converters.put(Instant.class, Instant::parse);
    }

    public Builder<T> addConverter(Class type, Function<String, Object> converter){
      converters.put(type, converter);
      return this;
    }

    public Builder<T> baseQuery(Query<T> baseQuery) {
      this.baseQuery = baseQuery;
      return this;
    }

    public Builder<T> naturalOrdering(OrderBuilder naturalOrdering) {
      this.naturalOrdering = naturalOrdering;
      return this;
    }

    public CursorQueryWrapper<T> build() {
      if (baseQuery == null) {
        throw new MissingBaseQueryException();
      }

      return new CursorQueryWrapper<>(baseQuery, getNaturalOrderingProperty(naturalOrdering), converters);
    }

    private Property getNaturalOrderingProperty(OrderBuilder naturalOrdering) {
      if (naturalOrdering == null) {
        throw new InvalidNaturalOrderingException();
      }

      List<Property> orderingProperties = naturalOrdering.getProperties();
      if (orderingProperties.size() != 1) {
        throw new InvalidNaturalOrderingException();
      }

      return orderingProperties.get(0);
    }
  }

  private Query<T> query;
  private CursorQuery cursorQuery;
  private Property naturalOrdering;
  private final Map<Class,Function<String,Object>> converters;
  private boolean reversed;

  private CursorQueryWrapper(Query<T> query, Property naturalOrdering,
      Map<Class, Function<String, Object>> converters) {
    this.query = query;
    this.cursorQuery = new CursorQuery();
    this.naturalOrdering = naturalOrdering;
    this.converters = converters;
  }

  public CursorQuery cursor() {
    return cursorQuery;
  }

  public CursedList<T> findCursedList() {
    String naturalOrderingColumn = naturalOrdering.getProperty();

    if (cursorQuery.orderBuilder == null) {
      cursorQuery.orderBuilder = order();
    }

    if (cursorQuery.orderBuilder.getProperties().stream()
        .noneMatch(property -> property.getProperty().equals(naturalOrderingColumn))) {
      if (naturalOrdering.isAscending()) {
        cursorQuery.orderBuilder.asc(naturalOrderingColumn);
      } else {
        cursorQuery.orderBuilder.desc(naturalOrderingColumn);
      }
    }

    // first query for total count and then apply cursorQuery criteria and find list
    int count = query.findCount();
    try {
      return cursedListFactory(count, findList(query.copy()));
    } catch (InvalidBeanException e) {
      throw e;
    } catch (Exception e) {
      throw new CursedQueryException(e);
    }
  }

  private List<Edge<T>> findList(Query<T> clone) {
    List<Property> orderingList = cursorQuery.orderBuilder
        .getProperties();

    if (cursorQuery.first != null) {
      clone.orderBy(ordering(false));
      clone.setMaxRows(cursorQuery.first + 1);

      decodeCursor(cursorQuery.after)
          .ifPresent(cursor -> buildCloneQuery(clone, orderingList, cursor));
    } else if (cursorQuery.last != null) {
      clone.orderBy(ordering(true));
      clone.setMaxRows(cursorQuery.last + 1);

      decodeCursor(cursorQuery.before)
          .ifPresent(cursor -> buildCloneQuery(clone, orderingList, cursor));
    } else {
      clone.orderBy(ordering(false));
    }

    return clone.findList().stream()
        .map(item -> new Edge<>(item, makeCursor(item)))
        .collect(Collectors.toList());
  }

  private void buildCloneQuery(Query<T> clone, List<Property> orderingList, Cursor cursor) {
    if (!validateCursor(orderingList, cursor)) {
      logger.debug(
          "Ignoring cursor because cursor and order do not contain the exact same properties.");
      return;
    }

    Iterator<Property> ordering = orderingList.iterator();
    if (ordering.hasNext()) {
      applyCriteria(clone.where(), cursor, ordering);
    }
  }

  private boolean validateCursor(List<Property> orderingList, Cursor cursor) {
    if (orderingList.size() != cursor.getArgs().size()) {
      return false;
    }

    for (Map.Entry<String, Object> arg : cursor.getArgs().entrySet()) {
      if (orderingList.stream()
          .noneMatch(property -> property.getProperty().equals(arg.getKey()))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Inspired by https://stackoverflow.com/a/38017813/2694806
   */
  private void applyCriteria(ExpressionList<T> expr, Cursor cursor, Iterator<Property> ordering) {
    Property orderingProperty = ordering.next();
    boolean asc = orderingProperty.isAscending();
    String col = orderingProperty.getProperty();
    Object val = getColumnValue(expr.query(), cursor, col);

    if (ordering.hasNext() && val == null) {
      applyCriteria(expr, cursor, ordering);
      return;
    }

    if (!ordering.hasNext()) {
      if (asc) {
        expr.gt(col, val);
      } else {
        expr.lt(col, val);
      }
      return;
    }

    ExpressionList<T> subExpr;
    if (asc) {
      subExpr = expr.ge(col, val).or().gt(col, val);
    } else {
      subExpr = expr.le(col, val).or().lt(col, val);
    }

    applyCriteria(subExpr.and(), cursor, ordering);

    subExpr.endOr().endAnd();
  }

  private Object getColumnValue(Query<T> query, Cursor cursor, String col) {
    Collection<BeanProperty> props = ((DefaultOrmQuery<T>) query)
        .getBeanDescriptor()
        .propertiesAll();

    BeanProperty prop = props.stream()
        .filter(p -> p.getName().equals(col))
        .findFirst()
        .orElseThrow(() -> new BadCursorArgumentException(props, col));

    Class type = prop.getScalarType().getType();
    Function<String, Object> converter = Optional.ofNullable(converters.get(type))
        .orElseThrow(() -> new MissingConverterException(type));

    return cursor.get(col, converter);
  }

  private Cursor makeCursor(T item) {
    Map<String, Object> args = new HashMap<>();

    for (Property orderProperty : cursorQuery.orderBuilder.getProperties()) {
      String key = orderProperty.getProperty();
      getValueFromBean(item, key).ifPresent(value -> args.put(key, value));
    }

    return new Cursor(item.getClass().getSimpleName(), args);
  }

  private Optional<Object> getValueFromBean(T item, String key) {
    String method = String.format("get%s", StringUtils.capitalize(key));
    Object value;

    try {
      value = item.getClass().getMethod(method).invoke(item);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new InvalidBeanException(method, item.getClass());
    }

    return Optional.ofNullable(value);
  }

  private String ordering(boolean reverse) {
    this.reversed = reverse;

    return CursorOrdering.make(reverse, cursorQuery.orderBuilder);
  }

  private CursedList<T> cursedListFactory(int count, List<Edge<T>> rows) {
    Boolean hasNext = null, hasPrev = null;

    if (cursorQuery.first != null) {
      hasNext = rows.size() > cursorQuery.first;
      if (!rows.isEmpty() && hasNext) {
        rows.remove(rows.size() - 1);
      }
      if (cursorQuery.after == null) {
        hasPrev = false;
      }
    } else if (cursorQuery.last != null) {
      hasPrev = rows.size() > cursorQuery.last;
      if (!rows.isEmpty() && hasPrev) {
        rows.remove(rows.size() - 1);
      }
      if (cursorQuery.before == null) {
        hasNext = false;
      }
    }

    if (reversed) {
      Collections.reverse(rows);
    }

    return new CursedList<>(count, rows, hasNext, hasPrev);
  }

  public static class CursorQuery {

    private Integer first, last;
    private String after, before;
    private OrderBuilder orderBuilder;

    public CursorQuery first(Integer first) {
      this.first = first;
      return this;
    }

    public CursorQuery last(Integer last) {
      this.last = last;
      return this;
    }

    public CursorQuery after(String after) {
      this.after = after;
      return this;
    }

    public CursorQuery before(String before) {
      this.before = before;
      return this;
    }

    public CursorQuery order(OrderBuilder orderBuilder) {
      this.orderBuilder = orderBuilder;
      return this;
    }
  }

  public static class CursedList<T> {

    private int count;
    private List<Edge<T>> rows;
    private Boolean hasNext, hasPrev;

    CursedList(int count, List<Edge<T>> rows, Boolean hasNext, Boolean hasPrev) {
      this.count = count;
      this.rows = rows;
      this.hasNext = hasNext;
      this.hasPrev = hasPrev;
    }

    public List<Edge<T>> getList() {
      return rows;
    }

    public int getTotalCount() {
      return count;
    }

    public Boolean hasNext() {
      return hasNext;
    }

    public Boolean hasPrev() {
      return hasPrev;
    }
  }
}
