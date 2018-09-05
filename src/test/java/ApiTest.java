import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.asc;
import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.desc;
import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.order;
import static util.assertions.SequenceAssertions.assertRange;
import static util.assertions.SequenceAssertions.range;
import static util.assertions.SequenceAssertions.sequence;

import io.ebean.Ebean;
import javax.persistence.PersistenceException;
import me.paulschwarz.ebean.cursorpagination.CursorQueryWrapper;
import me.paulschwarz.ebean.cursorpagination.CursorQueryWrapper.CursedList;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding;
import me.paulschwarz.ebean.cursorpagination.exceptions.InvalidNaturalOrderingException;
import util.BaseTestCase;
import util.assertions.SequenceAssertions.Expectations;
import util.factories.ExampleFactory;
import util.models.Example;

public class ApiTest extends BaseTestCase {

  @Override
  protected void setUp() {
    super.setUp();
    new ExampleFactory().save(100);
  }

  private CursorQueryWrapper<Example> getExampleCursorQueryWrapper() {
    return new CursorQueryWrapper<>(Ebean.find(Example.class), desc("id"));
  }

  private void assertResult(CursedList<Example> cursedList, Expectations<Integer> expectations,
      Boolean expectedPrev, Boolean expectedNext) {
    System.out.println(cursedList.getList());

    // https://facebook.github.io/relay/graphql/connections.htm#sec-undefined.PageInfo.Fields
    // When the server cannot efficiently determine that elements exist before/after, return null.
    assertEquals(100, cursedList.getTotalCount());
    assertRange(expectations, cursedList.getList(), edge -> edge.getNode().getId());
    assertEquals("Unexpected next value.", expectedNext, cursedList.hasNext());
    assertEquals("Unexpected prev value.", expectedPrev, cursedList.hasPrev());
  }

  public void testCursorPaginationSync_getWithFirst() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .first(10);

    assertResult(queryWrapper.findCursedList(), range(100, 91), false, true);
  }

  public void testCursorPaginationSync_getWithManyFirst() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .first(1000);

    assertResult(queryWrapper.findCursedList(), range(100, 1), false, false);
  }

  public void testCursorPaginationSync_getWithLast() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .last(10);

    assertResult(queryWrapper.findCursedList(), range(10, 1), true, false);
  }

  public void testCursorPaginationSync_getWithManyLast() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .last(1000);

    assertResult(queryWrapper.findCursedList(), range(100, 1), false, false);
  }

  public void testCursorPaginationSync_getWithFirstAfter() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .first(10)
        .after(CursorEncoding.encode("Example{id(91)}"));

    assertResult(queryWrapper.findCursedList(), range(90, 81), null, true);
  }

  public void testCursorPaginationSync_getWithLastBefore() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .last(10)
        .before(CursorEncoding.encode("Example{id(70)}"));

    assertResult(queryWrapper.findCursedList(), range(80, 71), true, null);
  }

  public void testCursorPaginationSync_getWithSortAndFirst() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("rank"))
        .first(5);

    assertResult(queryWrapper.findCursedList(), sequence(2, 1, 5, 4, 3), false, true);
  }

  public void testCursorPaginationSync_getWithSortAndFirstAndInvalidAfterCursor() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("rank"))
        .first(5)
        .after(CursorEncoding.encode("Example{rank(32)}"));

    // Technically, we could compute that hasPrev is false since the after cursor is invalid.
    // It's not a problem worth solving because the cursor is wrong in the first place.
    assertResult(queryWrapper.findCursedList(), sequence(2, 1, 5, 4, 3), null, true);
  }

  public void testCursorPaginationSync_getWithSortAndFirstAfter() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("rank"))
        .first(5)
        .after(CursorEncoding.encode("Example{id(4)rank(32)}"));

    assertResult(queryWrapper.findCursedList(), sequence(3, 8, 7, 6, 11), null, true);
  }

  public void testCursorPaginationSync_getWithSortAndLastBefore() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("rank"))
        .last(5)
        .before(CursorEncoding.encode("Example{id(98)rank(1)}"));

    assertResult(queryWrapper.findCursedList(), sequence(91, 90, 95, 94, 93), true, null);
  }

  public void testCursorPaginationSync_getWithTripleSort() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(asc("parity").desc("rank").desc("id"))
        .first(5)
        .after(CursorEncoding.encode("Example{id(10)rank(30)parity(odd)}"));

    assertResult(queryWrapper.findCursedList(), sequence(14, 12, 16, 20, 18), null, true);
  }

  public void testCursorPaginationSync_getWithSortByDate() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("createdAt"))
        .first(1)
        .after(CursorEncoding.encode("Example{id(10)createdAt(2010-01-01)}"));

    queryWrapper.findCursedList();
  }

  public void testCursorPaginationSync_beanException() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("updatedAt"))
        .first(1)
        .after(CursorEncoding.encode("Example{id(10)updatedAt(2020-01-01)}"));

    try {
      queryWrapper.findCursedList();
      fail("Expected exception to be thrown");
    } catch (RuntimeException e) {
      String expectedException = String
          .format("Failed to call getUpdatedAt() on instance of %s", Example.class.getName());
      assertEquals(expectedException, e.getMessage());
    }
  }

  public void testCursorPaginationSync_sqlExceptionDate() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("createdAt"))
        .first(1)
        .after(CursorEncoding.encode("Example{id(10)createdAt(foo)}"));

    try {
      queryWrapper.findCursedList();
      fail("Expected exception to be thrown");
    } catch (PersistenceException e) {
      assertTrue(e.getMessage().contains("Cannot parse \"TIMESTAMP\" constant \"foo\""));
    }
  }

  public void testCursorPaginationSync_sqlException() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("deletedAt"));

    try {
      queryWrapper.findCursedList();
      fail("Expected exception to be thrown");
    } catch (PersistenceException e) {
      assertTrue(e.getMessage().contains("Column \"DELETEDAT\" not found"));
    }
  }

  public void testCursorPaginationSync_invalidNaturalOrdering() {
    try {
      new CursorQueryWrapper<>(Ebean.find(Example.class), order().desc("foo").asc("bar"));
      fail("Expected exception to be thrown");
    } catch (InvalidNaturalOrderingException e) {
      assertEquals(
          "When constructing a CursorQueryWrapper, you must specify a natural ordering. Use asc(col) or desc(col). Supply exactly one natural ordering column. This column should be 1) sortable 2) unique 3) immutable.",
          e.getMessage());
    }
  }
}
