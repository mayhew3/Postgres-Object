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

  public SQLConnection createConnection(String dbhost, String dbuser, String dbpassword) throws URISyntaxException, SQLException {
    return new MySQLConnection(initiateDBConnect(dbhost, dbuser, dbpassword));
  }

  private Connection initiateDBConnect(String dbhost, String dbuser, String dbpassword) {
    logger.log(Level.INFO, "Initializing connection.");

    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      System.out.println("Cannot find MySQL drivers. Exiting.");
      throw new RuntimeException(e.getLocalizedMessage());
    }

    try {
      return DriverManager.getConnection(dbhost, dbuser, dbpassword);
    } catch (SQLException e) {
      System.out.println("Cannot connect to database. Exiting.");
      throw new RuntimeException(e.getLocalizedMessage());
    }
  }
}
