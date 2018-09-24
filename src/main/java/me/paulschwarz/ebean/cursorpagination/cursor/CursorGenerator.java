package me.paulschwarz.ebean.cursorpagination.cursor;

import static me.paulschwarz.ebean.cursorpagination.CursorUtil.getValueFromBean;

import io.ebean.OrderBy.Property;
import java.util.HashMap;
import java.util.Map;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding.Cursor;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.OrderBuilder;

public class CursorGenerator {

  private OrderBuilder ordering;

  public CursorGenerator(OrderBuilder ordering) {
    this.ordering = ordering;
  }

  public <T> Cursor makeCursor(T item) {
    Map<String, Object> args = new HashMap<>();

    for (Property property : ordering.getProperties()) {
      getValueFromBean(item, property.getProperty())
          .ifPresent(value -> args.put(property.getProperty(), value));
    }

    return new Cursor(item.getClass().getSimpleName(), args);
  }

  public <T> String makeEncodedCursor(T item) {
    return makeCursor(item).encode();
  }
}
