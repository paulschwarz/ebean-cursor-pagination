package me.paulschwarz.ebean.cursorpagination.cursor;

import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.asc;
import static org.junit.Assert.assertEquals;

import io.ebean.Ebean;
import io.ebean.Query;
import org.junit.Before;
import org.junit.Test;
import util.models.Example;

public class CursorGeneratorTest {

  private Query<Example> query;

  @Before
  public void createQuery() {
    query = Ebean.find(Example.class);
  }

  @Test
  public void testCursorGenerator() {
    CursorGenerator<Example> cursorGenerator = new CursorGenerator<>(query,
        asc("rank").desc("parity"));

    assertEquals(CursorEncoding.encode("Example{parity(odd)rank(5)}"),
        cursorGenerator.makeEncodedCursor(new Example(5, "odd")));
  }
}
