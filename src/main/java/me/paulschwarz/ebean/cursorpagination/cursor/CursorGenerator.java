package me.paulschwarz.ebean.cursorpagination.cursor;

import static me.paulschwarz.ebean.cursorpagination.CursorUtil.getValueFromBean;

import io.ebean.OrderBy.Property;
import io.ebean.Query;
import java.util.HashMap;
import java.util.Map;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding.Cursor;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.OrderBuilder;

public class CursorGenerator<T> {

  private Query<T> query;
  private OrderBuilder ordering;

  public CursorGenerator(Query<T> query, OrderBuilder ordering) {
    this.ordering = ordering;
    this.query = query;
  }

  public Cursor makeCursor(T item) {
    Map<String, Object> args = new HashMap<>();

    for (Property property : ordering.getProperties()) {
      getValueFromBean(query, item, property.getProperty())
          .ifPresent(value -> args.put(property.getProperty(), value));
    }

    return new Cursor(item.getClass().getSimpleName(), args);
  }

  public String makeEncodedCursor(T item) {
    return makeCursor(item).encode();
  }
}
