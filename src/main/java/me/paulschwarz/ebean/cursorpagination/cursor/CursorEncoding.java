package me.paulschwarz.ebean.cursorpagination.cursor;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CursorEncoding {

  private static final Pattern PATTERN_CLASS_ARGS = Pattern.compile("([A-Z]\\w+)\\{(.*)}");
  private static final Pattern PATTERN_EACH_ARG = Pattern.compile("(\\w+)\\(([^(]*)\\)");

  public static String encode(String value) {
    return Base64.getEncoder().encodeToString(value.getBytes());
  }

  public static String decode(String value) {
    try {
      return new String(Base64.getDecoder().decode(value));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static Optional<Cursor> decodeCursor(String cursor) {
    if (cursor == null || cursor.isEmpty()) {
      return Optional.empty();
    }

    String value = decode(cursor);

    if (value == null || value.isEmpty()) {
      return Optional.empty();
    }

    String type = null;
    String argsString = null;
    Map<String, Object> args = new HashMap<>();

    Matcher matcher = PATTERN_CLASS_ARGS.matcher(value);
    if (matcher.find()) {
      type = matcher.group(1);
      argsString = matcher.group(2);
    }

    if (type != null && argsString != null) {
      matcher = PATTERN_EACH_ARG.matcher(argsString);

      while (matcher.find()) {
        args.put(matcher.group(1), matcher.group(2));
      }
    }

    return Optional.of(new Cursor(type, args));
  }

  public static class Cursor {

    String type;
    Map<String, Object> args;

    public Cursor(String type, Map<String, Object> args) {
      this.type = type;
      this.args = args;
    }

    public String getType() {
      return type;
    }

    public <T> T get(String key, Function<String, T> converter) {
      if (args == null) {
        return null;
      }

      return converter.apply(String.valueOf(args.get(key)));
    }

    public Long getId() {
      return get("id", Long::parseLong);
    }

    public String encode() {
      return CursorEncoding.encode(toString());
    }

    @Override
    public String toString() {
      StringBuilder output = new StringBuilder(type).append("{");

      for (Map.Entry item : args.entrySet()) {
        output.append(String.format("%s(%s)", item.getKey(), item.getValue()));
      }

      output.append("}");

      return output.toString();
    }
  }
}
