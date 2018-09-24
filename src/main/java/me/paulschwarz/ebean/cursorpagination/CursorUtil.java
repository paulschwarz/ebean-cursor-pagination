package me.paulschwarz.ebean.cursorpagination;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import me.paulschwarz.ebean.cursorpagination.exceptions.InvalidBeanException;
import org.apache.commons.lang3.StringUtils;

public class CursorUtil {

  public static <T> Optional<Object> getValueFromBean(T item, String key) {
    String method = String.format("get%s", StringUtils.capitalize(key));
    Object value;

    try {
      value = item.getClass().getMethod(method).invoke(item);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new InvalidBeanException(method, item.getClass());
    }

    return Optional.ofNullable(value);
  }
}
