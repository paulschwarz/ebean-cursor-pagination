package me.paulschwarz.ebean.cursorpagination.exceptions;

import io.ebeaninternal.server.deploy.BeanProperty;
import java.util.Collection;

public class BadCursorArgumentException extends RuntimeException {

  public BadCursorArgumentException(Collection<BeanProperty> props, String col) {
    super(String.format("Couldn't find a field called \"%s\" in [%s]",
        col,
        props.stream()
            .map(BeanProperty::getName)
            .reduce(String::concat)
            .orElse("?")));
  }
}
