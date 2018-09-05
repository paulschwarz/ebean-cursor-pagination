package me.paulschwarz.ebean.cursorpagination.exceptions;

public class InvalidBeanException extends RuntimeException {

  public InvalidBeanException(String method, Class type) {
    super(String.format("Failed to call %s() on instance of %s", method, type.getName()));
  }
}
