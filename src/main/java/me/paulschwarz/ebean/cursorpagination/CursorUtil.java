package me.paulschwarz.ebean.cursorpagination;

import io.ebean.Query;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.querydefn.DefaultOrmQuery;
import java.util.Collection;
import java.util.Optional;
import me.paulschwarz.ebean.cursorpagination.exceptions.BadCursorArgumentException;

public class CursorUtil {

  public static <T> Optional<Object> getValueFromBean(Query<T> query, T item, String key) {
    BeanProperty prop = getBeanProperty(query, key);

    return Optional.ofNullable(prop.getVal(item));
  }

  public static <T> BeanProperty getBeanProperty(Query<T> query, String key) {
    Collection<BeanProperty> props = ((DefaultOrmQuery<T>) query)
        .getBeanDescriptor()
        .propertiesAll();

    return props.stream()
        .filter(p -> p.getName().equals(key))
        .findFirst()
        .orElseThrow(() -> new BadCursorArgumentException(props, key));
  }

  public static <T> String getBeanName(Query<T> query) {
    return query.getBeanType().getName();
  }
}
