package com.mayhew3.gamesutil.db;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgresConnectionFactory extends ConnectionFactory {

  @Override
  public SQLConnection createConnection() throws URISyntaxException, SQLException {
    return new PostgresConnection(initiateDBConnect());
  }

  private Connection initiateDBConnect() throws URISyntaxException, SQLException {
    String postgresURL = System.getenv("postgresURL");

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
}
