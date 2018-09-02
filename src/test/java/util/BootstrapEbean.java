package util;

import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.config.ServerConfig;
import io.ebean.config.dbplatform.h2.H2Platform;

class BootstrapEbean {

  private final ServerConfig config = new ServerConfig();

  BootstrapEbean() {
    config.loadFromProperties();
    config.setDatabasePlatform(new H2Platform());
  }

  EbeanServer create() {
    return EbeanServerFactory.create(config);
  }
}
