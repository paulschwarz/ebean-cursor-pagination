package util;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.models.Example;

public abstract class BaseTestCase extends TestCase {

  private static final Logger log = LoggerFactory.getLogger(BaseTestCase.class);

  static {
    BootstrapEbean bootstrapEbean = new BootstrapEbean();
    EbeanServer ebeanServer = bootstrapEbean.create();

    log.info("Bootstrap EbeanServer instance {}", ebeanServer.getName());
  }

  @Override
  protected void setUp() {
    Ebean.createQuery(Example.class).delete();

    Ebean.execute(Ebean.createSqlUpdate("ALTER TABLE example ALTER COLUMN id RESTART WITH 1"));
  }
}
