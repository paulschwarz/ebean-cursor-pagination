package me.paulschwarz.ebean.cursorpagination;

import me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding.Cursor;

public class Edge<T> {

  private final T node;
  private final Cursor cursor;

  Edge(T node, Cursor cursor) {
    this.node = node;
    this.cursor = cursor;
  }

  public T getNode() {
    return node;
  }

  public Cursor getCursor() {
    return cursor;
  }

  @Override
  public String toString() {
    return String.format("Edge(%s)", node);
  }
}
