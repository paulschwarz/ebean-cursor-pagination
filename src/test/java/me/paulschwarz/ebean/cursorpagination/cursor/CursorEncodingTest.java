package me.paulschwarz.ebean.cursorpagination.cursor;

import static me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding.decode;
import static me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding.encode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import me.paulschwarz.ebean.cursorpagination.cursor.CursorEncoding.Cursor;

public class CursorEncodingTest extends TestCase {

  public void testBase64() {
    assertEquals("foo", decode(encode("foo")));
  }

  public void testBase64_badFormat() {
    assertNull(decode("0"));
  }

  public void testCursorEncoding() {
    Map<String, Object> args = new HashMap<>();
    args.put("id", 100);
    args.put("created", Instant.EPOCH);
    CursorEncoding.Cursor cursor = new CursorEncoding.Cursor("Example", args);

    String expected = "Example{created(1970-01-01T00:00:00Z)id(100)}";
    assertEquals(expected, cursor.toString());
    assertEquals(encode(expected), cursor.encode());
    assertEquals("Example", cursor.getType());
  }

  public void testCursorDecode_null() {
    assertFalse(CursorEncoding.decodeCursor("").isPresent());
  }

  public void testCursorDecode() {
    String encoded = encode("Example{created(1970-01-01T00:00:00Z)id(100)}");
    Cursor cursor = CursorEncoding.decodeCursor(encoded).get();

    assertEquals("Example", cursor.getType());
    assertEquals(Integer.valueOf(100), cursor.get("id", Integer::parseInt));
    assertEquals(Long.valueOf(100), cursor.getId());
    assertEquals(Instant.EPOCH, cursor.get("created", Instant::parse));
  }
}
