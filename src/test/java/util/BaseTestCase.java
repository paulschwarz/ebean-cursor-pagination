package util;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.models.Example;

public abstract class BaseTestCase {

  private static final Logger log = LoggerFactory.getLogger(BaseTestCase.class);

  static {
    BootstrapEbean bootstrapEbean = new BootstrapEbean();
    EbeanServer ebeanServer = bootstrapEbean.create();

    log.info("Bootstrap EbeanServer instance {}", ebeanServer.getName());
  }

  @Before
  public void clearDb() {
    Ebean.createQuery(Example.class).delete();

    Ebean.execute(Ebean.createSqlUpdate("ALTER TABLE example ALTER COLUMN id RESTART WITH 1"));
  }
}
