package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgresConnectionFactory {

  private static final Logger logger = LogManager.getLogger(PostgresConnectionFactory.class);

  private static String schemaName = null;

  public static PostgresConnection createConnection(DatabaseEnvironment databaseEnvironment) throws MissingEnvException, URISyntaxException, SQLException {
    String databaseUrl = databaseEnvironment.getDatabaseUrl();
    if (databaseUrl == null) {
      throw new IllegalStateException("Null database URL from database environment: " + databaseEnvironment.getEnvironmentName());
    }
    PostgresConnectionFactory.schemaName = databaseEnvironment.getSchemaName();
    return initiateDBConnect(databaseUrl);
  }

  @Deprecated(since = "0.13.3")
  public static PostgresConnection initiateDBConnect(String postgresURL) throws URISyntaxException, SQLException {
    debug("Connecting to: " + postgresURL);
    try {
      Connection connection = DriverManager.getConnection(postgresURL);
      return new PostgresConnection(connection, postgresURL, PostgresConnectionFactory.schemaName);
    } catch (SQLException e) {
      URI dbUri = new URI(postgresURL);

      String username = dbUri.getUserInfo().split(":")[0];
      String password = dbUri.getUserInfo().split(":")[1];
      String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() +
          "?user=" + username + "&password=" + password + "&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";

      logger.debug("Drivers found: ");
      DriverManager.drivers()
          .forEach(driver -> logger.debug(" - " + driver.toString()));

      DriverManager.registerDriver(new org.postgresql.Driver());

      logger.debug("Drivers found (after registering Postgres): ");
      DriverManager.drivers()
          .forEach(driver -> logger.debug(" - " + driver.toString()));

      Connection connection = DriverManager.getConnection(dbUrl);
      return new PostgresConnection(connection, dbUrl, PostgresConnectionFactory.schemaName);
    }
  }

  private static void debug(String msg) {
    logger.debug(msg);
  }

}
