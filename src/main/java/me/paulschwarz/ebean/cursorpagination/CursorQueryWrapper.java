package me.paulschwarz.ebean.cursorpagination;

import static me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding.decodeCursor;
import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.order;

import io.ebean.ExpressionList;
import io.ebean.OrderBy.Property;
import io.ebean.Query;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding.Cursor;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.OrderBuilder;

public class CursorQueryWrapper<T> {

  private Query<T> query;
  private CursorQuery cursorQuery;
  private Property naturalOrdering;
  private boolean reversed;

  public CursorQueryWrapper(Query<T> query, OrderBuilder naturalOrdering) {
    this.query = query;
    this.cursorQuery = new CursorQuery();
    this.naturalOrdering = naturalOrdering.getProperties().get(0);
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
    return cursedListFactory(count, findList(query.copy()));
  }

  private List<T> findList(Query<T> clone) {
    Iterator<Property> ordering = cursorQuery.orderBuilder
        .getProperties()
        .iterator();

    if (cursorQuery.first != null) {
      clone.orderBy(ordering(false));
      clone.setMaxRows(cursorQuery.first + 1);

      decodeCursor(cursorQuery.after)
          .ifPresent(cursor -> {
            if (ordering.hasNext()) {
              buildQuery(clone.where(), cursor, ordering);
            }
          });
    }

    if (cursorQuery.last != null) {
      clone.orderBy(ordering(true));
      clone.setMaxRows(cursorQuery.last + 1);

      decodeCursor(cursorQuery.before)
          .ifPresent(cursor -> {
            if (ordering.hasNext()) {
              buildQuery(clone.where(), cursor, ordering);
            }
          });
    }

    return clone.findList();
  }

  /**
   * Inspired by https://stackoverflow.com/a/38017813/2694806
   */
  private void buildQuery(ExpressionList<T> expr, Cursor cursor, Iterator<Property> ordering) {
    Property orderingProperty = ordering.next();
    boolean asc = orderingProperty.isAscending();
    String col = orderingProperty.getProperty();
    String val = cursor.get(col, String::toString);

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

    buildQuery(subExpr.and(), cursor, ordering);

    subExpr.endOr().endAnd();
  }

  private String ordering(boolean reverse) {
    this.reversed = reverse;

    return CursorOrdering.make(reverse, cursorQuery.orderBuilder);
  }

  private CursedList<T> cursedListFactory(int count, List<T> rows) {
    boolean hasNext = false;
    if (cursorQuery.first != null) {
      hasNext = rows.size() > cursorQuery.first;
      if (!rows.isEmpty() && hasNext) {
        rows.remove(rows.size() - 1);
      }
    }

    boolean hasPrev = false;
    if (cursorQuery.last != null) {
      hasPrev = rows.size() > cursorQuery.last;
      if (!rows.isEmpty() && hasPrev) {
        rows.remove(rows.size() - 1);
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
    private List<T> rows;
    private boolean hasNext, hasPrev;

    CursedList(int count, List<T> rows, boolean hasNext, boolean hasPrev) {
      this.count = count;
      this.rows = rows;
      this.hasNext = hasNext;
      this.hasPrev = hasPrev;
    }

    public List<T> getList() {
      return rows;
    }

    public int getTotalCount() {
      return count;
    }

    public boolean hasNext() {
      return hasNext;
    }

    public boolean hasPrev() {
      return hasPrev;
    }
  }
}
