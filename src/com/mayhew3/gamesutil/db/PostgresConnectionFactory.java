package com.mayhew3.gamesutil.db;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgresConnectionFactory extends ConnectionFactory {

  String postgresURL;

  @Override
  public SQLConnection createConnection() throws URISyntaxException, SQLException {
    postgresURL = System.getenv("postgresURL");
    return new PostgresConnection(initiateDBConnect());
  }

  @Override
  public SQLConnection createLocalConnection() throws URISyntaxException, SQLException {
    postgresURL = System.getenv("postgresURL_local");
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
