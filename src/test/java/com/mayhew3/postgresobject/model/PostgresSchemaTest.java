package com.mayhew3.postgresobject.model;

import com.mayhew3.postgresobject.db.PostgresConnectionFactory;
import com.mayhew3.postgresobject.db.SQLConnection;

import java.net.URISyntaxException;
import java.sql.SQLException;

@SuppressWarnings("unused")
public abstract class PostgresSchemaTest extends SchemaTest {

  public abstract String getDBConnectionString();

  @Override
  public SQLConnection createConnection() throws URISyntaxException, SQLException {
    String database_url = getDBConnectionString();
    return PostgresConnectionFactory.initiateDBConnect(database_url);
  }
}
