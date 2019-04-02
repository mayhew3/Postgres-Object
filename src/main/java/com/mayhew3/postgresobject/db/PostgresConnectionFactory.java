package com.mayhew3.postgresobject.db;

import com.google.common.collect.Lists;
import com.mayhew3.postgresobject.ArgumentChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

public enum PostgresConnectionFactory {
  HEROKU("heroku", "postgresURL_heroku"),
  LOCAL("local", "postgresURL_local"),
  TEST("test", "postgresURL_local_test"),
  DEMO("demo", "postgresURL_local_demo");

  private final String nickname;
  private final String envName;

  private static Logger logger = LogManager.getLogger(PostgresConnectionFactory.class);

  PostgresConnectionFactory(String nickname, String envName) {
    this.nickname = nickname;
    this.envName = envName;
  }

  public static SQLConnection createConnection(ArgumentChecker argumentChecker) throws URISyntaxException, SQLException {
    String dbIdentifier = argumentChecker.getDBIdentifier();
    Optional<PostgresConnectionFactory> connectionType = getConnectionType(dbIdentifier);
    if (connectionType.isPresent()) {
      PostgresConnectionFactory postgresConnectionFactory = connectionType.get();
      return getSqlConnection(postgresConnectionFactory);
    } else {
      throw new IllegalStateException("No connection type with nickname: " + dbIdentifier);
    }
  }

  @NotNull
  public static SQLConnection getSqlConnection(PostgresConnectionFactory postgresConnectionFactory) throws URISyntaxException, SQLException {
    String postgresURL = System.getenv(postgresConnectionFactory.envName);
    if (postgresURL == null) {
      throw new IllegalStateException("No environment variable found with name: " + postgresConnectionFactory.envName);
    }
    return initiateDBConnect(postgresURL);
  }

  public static Optional<PostgresConnectionFactory> getConnectionType(final String nickname) {
    return Lists.newArrayList(PostgresConnectionFactory.values())
        .stream()
        .filter(postgresConnectionFactory -> postgresConnectionFactory.nickname.equalsIgnoreCase(nickname))
        .findAny();
  }

  public static PostgresConnection initiateDBConnect(String postgresURL) throws URISyntaxException, SQLException {
    debug("Connecting to: " + postgresURL);
    try {
      Connection connection = DriverManager.getConnection(postgresURL);
      return new PostgresConnection(connection, postgresURL);
    } catch (SQLException e) {
      URI dbUri = new URI(postgresURL);

      String username = dbUri.getUserInfo().split(":")[0];
      String password = dbUri.getUserInfo().split(":")[1];
      String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() +
          "?user=" + username + "&password=" + password + "&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";

      Connection connection = DriverManager.getConnection(dbUrl);
      return new PostgresConnection(connection, dbUrl);
    }
  }

  private static void debug(String msg) {
    logger.debug(msg);
  }

}
