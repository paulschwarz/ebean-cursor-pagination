package me.paulschwarz.ebean.cursorpagination;

import me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding.Cursor;

public class Edge<T> {

  private final T node;
  private final Cursor cursor;

  public Edge(T node, Cursor cursor) {
    this.node = node;
    this.cursor = cursor;
  }

  public T getNode() {
    return node;
  }

  public Cursor getCursor() {
    return cursor;
  }
}
