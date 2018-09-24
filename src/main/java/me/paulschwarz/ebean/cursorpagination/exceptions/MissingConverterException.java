package me.paulschwarz.ebean.cursorpagination.exceptions;

public class MissingConverterException extends RuntimeException {

  public MissingConverterException(Class type, String beanName, String beanProp) {
    super(String.format("Missing converter for type \"%s\" (while accessing %s.%s)",
        type.getName(), beanName, beanProp));
  }
}
