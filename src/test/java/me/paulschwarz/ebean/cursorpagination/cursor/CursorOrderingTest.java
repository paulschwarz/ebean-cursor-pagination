package me.paulschwarz.ebean.cursorpagination.cursor;

import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.asc;
import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.desc;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CursorOrderingTest extends TestCase {

  public void testOrdering_default() {
    Assert.assertEquals("id desc", CursorOrdering.make(false, desc("id")));
    Assert.assertEquals("id", CursorOrdering.make(true, desc("id")));
  }

  public void testOrdering_id() {
    Assert.assertEquals("id", CursorOrdering.make(false, asc("id")));
    Assert.assertEquals("id desc", CursorOrdering.make(true, asc("id")));
  }

  public void testOrdering_created() {
    Assert.assertEquals("created desc, id desc",
        CursorOrdering.make(false, desc("created").desc("id")));
    Assert.assertEquals("created, id desc", CursorOrdering.make(false, asc("created").desc("id")));
    Assert.assertEquals("created, id", CursorOrdering.make(true, desc("created").desc("id")));
    Assert.assertEquals("created desc, id", CursorOrdering.make(true, asc("created").desc("id")));
  }

  public void testOrdering_id_created() {
    Assert.assertEquals("id, created desc", CursorOrdering.make(false, asc("id").desc("created")));
    Assert.assertEquals("id, created", CursorOrdering.make(false, asc("id").asc("created")));
    Assert.assertEquals("id desc, created", CursorOrdering.make(true, asc("id").desc("created")));
    Assert
        .assertEquals("id desc, created desc", CursorOrdering.make(true, asc("id").asc("created")));
  }
}
