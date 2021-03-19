package com.mayhew3.postgresobject.model;

import com.mayhew3.postgresobject.db.DatabaseEnvironment;
import com.mayhew3.postgresobject.db.PostgresConnectionFactory;
import com.mayhew3.postgresobject.db.SQLConnection;
import com.mayhew3.postgresobject.exception.MissingEnvException;

import java.net.URISyntaxException;
import java.sql.SQLException;

@SuppressWarnings("unused")
public abstract class PostgresSchemaTest extends SchemaTest {

  public abstract DatabaseEnvironment getDatabaseEnvironment();

  @Override
  public SQLConnection createConnection() throws URISyntaxException, SQLException, MissingEnvException {
    return PostgresConnectionFactory.createConnection(getDatabaseEnvironment());
  }
}
