package me.paulschwarz.ebean.cursorpagination.cursor;

import io.ebean.OrderBy.Property;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CursorOrdering {

  public static String make(boolean reverse, OrderBuilder orderBuilder) {
    if (reverse) {
      orderBuilder.reverse();
    }

    return orderBuilder.toString();
  }

  public static OrderBuilder desc(String property) {
    return order().desc(property);
  }

  public static OrderBuilder asc(String property) {
    return order().asc(property);
  }

  public static OrderBuilder order() {
    return new OrderBuilder();
  }

  public static class OrderBuilder {

    private List<Property> properties = new ArrayList<>();

    public OrderBuilder asc(String property) {
      properties.add(new Property(property, true));
      return this;
    }

    public OrderBuilder desc(String property) {
      properties.add(new Property(property, false));
      return this;
    }

    public List<Property> getProperties() {
      return properties;
    }

    @Override
    public String toString() {
      return String.join(", ",
          properties.stream()
              .map(Property::toString)
              .collect(Collectors.toList()));
    }

    private void reverse() {
      for (Property orderByProperty : properties) {
        orderByProperty.reverse();
      }
    }
  }
}
