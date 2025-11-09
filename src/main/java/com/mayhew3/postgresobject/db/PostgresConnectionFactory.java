package com.mayhew3.postgresobject.db;

import com.google.common.base.Joiner;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

  private static void maybeHandleDrivers() throws SQLException {

    logger.debug("Drivers found: ");
    DriverManager.drivers().forEach(driver -> logger.debug(" - " + driver.toString()));

    if (DriverManager.drivers().filter(driver -> driver.toString().contains("postgresql")).findAny().isEmpty()) {
      DriverManager.registerDriver(new org.postgresql.Driver());

      logger.debug("Drivers found (after registering Postgres): ");
      DriverManager.drivers().forEach(driver -> logger.debug(" - " + driver.toString()));
    }

  }

  @Deprecated(since = "0.13.3")
  public static PostgresConnection initiateDBConnect(String postgresURL) throws URISyntaxException, SQLException {

    maybeHandleDrivers();

    debug("Connecting to: " + postgresURL);
    try {
      Connection connection = DriverManager.getConnection(postgresURL);

      // Set search_path if schema is specified to ensure tables are created in the correct schema
      if (PostgresConnectionFactory.schemaName != null) {
        try {
          connection.createStatement().execute("SET search_path TO " + PostgresConnectionFactory.schemaName);
          logger.debug("Set search_path to: {}", PostgresConnectionFactory.schemaName);
        } catch (SQLException e) {
          logger.warn("Failed to set search_path to {}: {}", PostgresConnectionFactory.schemaName, e.getMessage());
        }
      }

      return new PostgresConnection(connection, postgresURL, PostgresConnectionFactory.schemaName);
    } catch (SQLException e) {
      // Only retry with URI parsing if the URL is in URI format (postgres://...) not JDBC format (jdbc:postgresql://...)
      // JDBC URLs already have credentials in query parameters, so this retry logic doesn't apply
      if (postgresURL.startsWith("jdbc:")) {
        // Rethrow the original exception for JDBC URLs
        throw e;
      }

      URI dbUri = new URI(postgresURL);

      String userInfo = dbUri.getUserInfo();
      if (userInfo == null) {
        throw new SQLException("Database URL missing user credentials: " + postgresURL, e);
      }

      String username = userInfo.split(":")[0];
      String password = userInfo.split(":")[1];

      List<String> paramParts = new ArrayList<>();
      paramParts.add("user=" + username);
      paramParts.add("password=" + password);
      paramParts.add("sslmode=require");
      paramParts.add("characterEncoding=UTF-8");

      if (PostgresConnectionFactory.schemaName != null) {
        paramParts.add("currentSchema=" + PostgresConnectionFactory.schemaName);
      }

      String paramStr = "?" + Joiner.on("&").join(paramParts);

      String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() + paramStr;

      logger.info("Connecting to {}...", dbUrl);

      Connection connection = DriverManager.getConnection(dbUrl);
      return new PostgresConnection(connection, dbUrl, PostgresConnectionFactory.schemaName);
    }
  }

  private static void debug(String msg) {
    logger.debug(msg);
  }

}
