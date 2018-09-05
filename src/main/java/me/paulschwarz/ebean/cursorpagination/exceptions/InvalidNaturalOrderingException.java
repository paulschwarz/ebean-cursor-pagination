package me.paulschwarz.ebean.cursorpagination.exceptions;

public class InvalidNaturalOrderingException extends RuntimeException {

  public InvalidNaturalOrderingException() {
    super("When constructing a CursorQueryWrapper, you must specify a natural ordering. Use asc(col) or desc(col). Supply exactly one natural ordering column. This column should be 1) sortable 2) unique 3) immutable.");
  }
}
