import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.asc;
import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.desc;
import static util.assertions.SequenceAssertions.assertRange;
import static util.assertions.SequenceAssertions.range;
import static util.assertions.SequenceAssertions.sequence;

import me.paulschwarz.ebean.cursorpagination.CursorQueryWrapper;
import me.paulschwarz.ebean.cursorpagination.CursorQueryWrapper.CursedList;
import io.ebean.Ebean;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding;
import util.BaseTestCase;
import util.assertions.SequenceAssertions.Expectations;
import util.factories.ExampleFactory;
import util.models.Example;

public class ApiTest extends BaseTestCase {

  private ExampleFactory exampleFactory = new ExampleFactory();
  private CursorQueryWrapper<Example> queryWrapper;

  @Override
  protected void setUp() {
    super.setUp();
    exampleFactory.save(100);
    queryWrapper = new CursorQueryWrapper<>(Ebean.find(Example.class), desc("id"));
  }

  private void assertResult(CursedList<Example> cursedList, Expectations<Integer> expectations,
      Boolean expectedPrev, Boolean expectedNext) {
    // https://facebook.github.io/relay/graphql/connections.htm#sec-undefined.PageInfo.Fields
    // When the server cannot efficiently determine that elements exist before/after, return null.
    assertEquals(100, cursedList.getTotalCount());
    assertRange(expectations, cursedList.getList(), edge -> edge.getNode().getId());
    assertEquals("Unexpected next value.", expectedNext, cursedList.hasNext());
    assertEquals("Unexpected prev value.", expectedPrev, cursedList.hasPrev());
  }

  public void testCursorPaginationSync_getWithFirst() {
    queryWrapper.cursor()
        .first(10);

    assertResult(queryWrapper.findCursedList(), range(100, 91), false, true);
  }

  public void testCursorPaginationSync_getWithManyFirst() {
    queryWrapper.cursor()
        .first(1000);

    assertResult(queryWrapper.findCursedList(), range(100, 1), false, false);
  }

  public void testCursorPaginationSync_getWithLast() {
    queryWrapper.cursor()
        .last(10);

    assertResult(queryWrapper.findCursedList(), range(10, 1), true, false);
  }

  public void testCursorPaginationSync_getWithManyLast() {
    queryWrapper.cursor()
        .last(1000);

    assertResult(queryWrapper.findCursedList(), range(100, 1), false, false);
  }

  public void testCursorPaginationSync_getWithFirstAfter() {
    queryWrapper.cursor()
        .first(10)
        .after(CursorEncoding.encode("Example{id(91)}"));

    assertResult(queryWrapper.findCursedList(), range(90, 81), null, true);
  }

  public void testCursorPaginationSync_getWithLastBefore() {
    queryWrapper.cursor()
        .last(10)
        .before(CursorEncoding.encode("Example{id(70)}"));

    assertResult(queryWrapper.findCursedList(), range(80, 71), true, null);
  }

  public void testCursorPaginationSync_getWithSortAndFirst() {
    queryWrapper.cursor()
        .order(desc("rank"))
        .first(5);

    assertResult(queryWrapper.findCursedList(), sequence(2, 1, 5, 4, 3), false, true);
  }

  public void testCursorPaginationSync_getWithSortAndFirstAfter() {
    queryWrapper.cursor()
        .order(desc("rank"))
        .first(5)
        .after(CursorEncoding.encode("Example{id(4)rank(32)}"));

    assertResult(queryWrapper.findCursedList(), sequence(3, 8, 7, 6, 9), null, true);
  }

  public void testCursorPaginationSync_getWithSortAndLastBefore() {
    queryWrapper.cursor()
        .order(desc("rank"))
        .last(5)
        .before(CursorEncoding.encode("Example{id(98)rank(1)}"));

    assertResult(queryWrapper.findCursedList(), sequence(91, 90, 95, 94, 93), true, null);
  }

  public void testCursorPaginationSync_getWithTripleSort() {
    queryWrapper.cursor()
        .order(asc("parity").desc("rank").desc("id"))
        .first(5)
        .after(CursorEncoding.encode("Example{id(10)rank(30)parity(odd)}"));

    assertResult(queryWrapper.findCursedList(), sequence(14, 12, 16, 20, 18), null, true);
  }
}
