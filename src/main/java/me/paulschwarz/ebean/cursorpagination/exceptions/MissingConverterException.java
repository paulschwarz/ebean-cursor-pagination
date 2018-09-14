package me.paulschwarz.ebean.cursorpagination.exceptions;

public class MissingConverterException extends RuntimeException {

  public MissingConverterException(Class type) {
    super(String.format("Missing converter for %s", type.getName()));
  }
}
