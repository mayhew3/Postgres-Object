package com.mayhew3.postgresobject.db;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class MySQLConnectionFactory {

  protected final Logger logger = Logger.getLogger(MySQLConnectionFactory.class.getName());

  public SQLConnection createConnection(String dbhost, String dbuser, String dbpassword) {
    String jdbcUrl = buildJdbcUrl(dbhost);
    String[] pieces = dbhost.split("/");
    String schemaName = pieces[pieces.length-1];
    return new MySQLConnection(initiateDBConnect(jdbcUrl, dbuser, dbpassword), schemaName);
  }

  private String buildJdbcUrl(String dbhost) {
    // If already a JDBC URL, return as-is
    if (dbhost.startsWith("jdbc:")) {
      return dbhost;
    }

    // Otherwise, build JDBC URL from components
    // Expected format: host:port/database or just host/database
    String host;
    String port = "3306"; // default MySQL port
    String database = "projects"; // default database

    if (dbhost.contains("/")) {
      String[] parts = dbhost.split("/");
      String hostPort = parts[0];
      database = parts[parts.length - 1];

      if (hostPort.contains(":")) {
        String[] hostPortParts = hostPort.split(":");
        host = hostPortParts[0];
        port = hostPortParts[1];
      } else {
        host = hostPort;
      }
    } else if (dbhost.contains(":")) {
      String[] hostPortParts = dbhost.split(":");
      host = hostPortParts[0];
      port = hostPortParts[1];
    } else {
      host = dbhost;
    }

    return "jdbc:mysql://" + host + ":" + port + "/" + database;
  }

  private Connection initiateDBConnect(String jdbcUrl, String dbuser, String dbpassword) {
    logger.log(Level.INFO, "Initializing connection.");

    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      System.out.println("Cannot find MySQL drivers. Exiting.");
      throw new RuntimeException(e.getLocalizedMessage());
    }

    try {
      return DriverManager.getConnection(jdbcUrl, dbuser, dbpassword);
    } catch (SQLException e) {
      System.out.println("Cannot connect to database. Exiting.");
      throw new RuntimeException(e.getLocalizedMessage());
    }
  }
}
