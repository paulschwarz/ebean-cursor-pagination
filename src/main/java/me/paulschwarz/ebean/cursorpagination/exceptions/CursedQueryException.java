package me.paulschwarz.ebean.cursorpagination.exceptions;

public class CursedQueryException extends RuntimeException {

  public CursedQueryException(Throwable throwable) {
    super(throwable);
  }
}
