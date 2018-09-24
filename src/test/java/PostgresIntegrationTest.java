import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.asc;
import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.desc;
import static me.paulschwarz.ebean.cursorpagination.cursor.CursorOrdering.order;
import static org.junit.Assert.assertEquals;
import static util.assertions.SequenceAssertions.assertRange;
import static util.assertions.SequenceAssertions.range;
import static util.assertions.SequenceAssertions.sequence;

import io.ebean.Ebean;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import me.paulschwarz.ebean.cursorpagination.CursorQueryWrapper;
import me.paulschwarz.ebean.cursorpagination.CursorQueryWrapper.CursedList;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding;
import me.paulschwarz.ebean.cursorpagination.exceptions.CursedQueryException;
import me.paulschwarz.ebean.cursorpagination.exceptions.InvalidBeanException;
import me.paulschwarz.ebean.cursorpagination.exceptions.InvalidNaturalOrderingException;
import org.junit.Before;
import org.junit.Test;
import util.BaseTestCase;
import util.assertions.SequenceAssertions.Expectations;
import util.factories.ExampleFactory;
import util.models.Example;

public class PostgresIntegrationTest extends BaseTestCase {

  @Before
  public void seed() {
    new ExampleFactory().save(100);
  }

  private CursorQueryWrapper<Example> getExampleCursorQueryWrapper() {
    return CursorQueryWrapper.<Example>builder()
        .addConverter(Instant.class, val -> Instant.ofEpochMilli(Long.valueOf(val)))
        .baseQuery(Ebean.find(Example.class))
        .naturalOrdering(desc("id"))
        .build();
  }

  private static long epoch(String value) {
    return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        .atStartOfDay()
        .atOffset(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli();
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

  @Test
  public void testCursorPaginationSync_getWithFirst() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .first(10);

    assertResult(queryWrapper.findCursedList(), range(100, 91), false, true);
  }

  @Test
  public void testCursorPaginationSync_getWithManyFirst() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .first(1000);

    assertResult(queryWrapper.findCursedList(), range(100, 1), false, false);
  }

  @Test
  public void testCursorPaginationSync_getWithLast() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .last(10);

    assertResult(queryWrapper.findCursedList(), range(10, 1), true, false);
  }

  @Test
  public void testCursorPaginationSync_getWithManyLast() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .last(1000);

    assertResult(queryWrapper.findCursedList(), range(100, 1), false, false);
  }

  @Test
  public void testCursorPaginationSync_getWithFirstAfter() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .first(10)
        .after(CursorEncoding.encode("Example{id(81)}"));

    assertResult(queryWrapper.findCursedList(), range(80, 71), null, true);
  }

  @Test
  public void testCursorPaginationSync_getWithLastBefore() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .last(10)
        .before(CursorEncoding.encode("Example{id(70)}"));

    assertResult(queryWrapper.findCursedList(), range(80, 71), true, null);
  }

  @Test
  public void testCursorPaginationSync_getWithSortAndFirst() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("rank"))
        .first(5);

    assertResult(queryWrapper.findCursedList(), sequence(2, 1, 5, 4, 3), false, true);
  }

  @Test
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

  @Test
  public void testCursorPaginationSync_getWithSortAndFirstAfter() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("rank"))
        .first(5)
        .after(CursorEncoding.encode("Example{id(4)rank(32)}"));

    assertResult(queryWrapper.findCursedList(), sequence(3, 8, 7, 6, 11), null, true);
  }

  @Test
  public void testCursorPaginationSync_getWithSortAndLastBefore() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("rank"))
        .last(5)
        .before(CursorEncoding.encode("Example{id(98)rank(1)}"));

    assertResult(queryWrapper.findCursedList(), sequence(91, 90, 95, 94, 93), true, null);
  }

  @Test
  public void testCursorPaginationSync_getWithTripleSort() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(asc("parity").desc("rank").desc("id"))
        .first(5)
        .after(CursorEncoding.encode("Example{id(10)rank(30)parity(odd)}"));

    assertResult(queryWrapper.findCursedList(), sequence(14, 12, 16, 20, 18), null, true);
  }

  @Test
  public void testCursorPaginationSync_getWithSortByDate() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("createdAt"))
        .first(1)
        .after(CursorEncoding.encode("Example{id(10)createdAt(" + epoch("2010-01-01") + ")}"));

    queryWrapper.findCursedList();
  }

  @Test(expected = InvalidBeanException.class)
  public void testCursorPaginationSync_InvalidBeanException_updatedAtIsNotAccessible() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("updatedAt"))
        .first(1)
        .after(CursorEncoding.encode("Example{id(10)updatedAt(" + epoch("2020-01-01") + ")}"));

    queryWrapper.findCursedList();
  }

  @Test(expected = CursedQueryException.class)
  public void testCursorPaginationSync_invalidEpochMillis() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("createdAt"))
        .first(1)
        .after(CursorEncoding.encode("Example{id(10)createdAt(foo)}"));

    queryWrapper.findCursedList();
  }

  @Test(expected = CursedQueryException.class)
  public void testCursorPaginationSync_sqlException() {
    CursorQueryWrapper<Example> queryWrapper = getExampleCursorQueryWrapper();

    queryWrapper.cursor()
        .order(desc("deletedAt"));

    queryWrapper.findCursedList();
  }

  @Test(expected = InvalidNaturalOrderingException.class)
  public void testCursorPaginationSync_invalidNaturalOrdering() {
    CursorQueryWrapper.<Example>builder()
        .naturalOrdering(order().desc("foo").asc("bar"))
        .baseQuery(Ebean.find(Example.class))
        .build();
  }
}
