package com.mayhew3.gamesutil.db;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

public class PostgresConnectionFactory {

  private String postgresURL;

  private HashMap<String, String> programToEnv;

  public PostgresConnectionFactory() {
    programToEnv = new HashMap<>();
    programToEnv.put("heroku", "postgresURL_heroku");
    programToEnv.put("local", "postgresURL_local");
    programToEnv.put("test", "postgresURL_local_test");
    programToEnv.put("demo", "postgresURL_local_demo");
  }

  public SQLConnection createConnection(String identifier) throws URISyntaxException, SQLException {
    String envName = programToEnv.get(identifier);

    if (envName == null) {
      throw new IllegalArgumentException("Unknown factory identifier: " + identifier);
    }

    postgresURL = System.getenv(envName);

    if (postgresURL == null) {
      throw new IllegalStateException("No environment variable found with name: " + envName);
    }

    return new PostgresConnection(initiateDBConnect());
  }

  private Connection initiateDBConnect() throws URISyntaxException, SQLException {
    debug("Connecting to: " + postgresURL);
    try {
      return DriverManager.getConnection(postgresURL);
    } catch (SQLException e) {
      URI dbUri = new URI(postgresURL);

      String username = dbUri.getUserInfo().split(":")[0];
      String password = dbUri.getUserInfo().split(":")[1];
      String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() +
          "?user=" + username + "&password=" + password + "&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";

      return DriverManager.getConnection(dbUrl);
    }
  }

  private void debug(String msg) {
    System.out.println(msg);
  }

}
