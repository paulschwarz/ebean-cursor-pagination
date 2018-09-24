package util;

import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.config.ServerConfig;
import io.ebean.config.dbplatform.postgres.PostgresPlatform;

class BootstrapEbean {

  private final ServerConfig config = new ServerConfig();

  BootstrapEbean() {
    config.loadFromProperties();
    config.setDatabasePlatform(new PostgresPlatform());
  }

  EbeanServer create() {
    return EbeanServerFactory.create(config);
  }
}
