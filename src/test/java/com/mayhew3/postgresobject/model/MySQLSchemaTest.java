package com.mayhew3.postgresobject.model;

import com.mayhew3.postgresobject.db.MySQLConnectionFactory;
import com.mayhew3.postgresobject.db.SQLConnection;

@SuppressWarnings("unused")
public abstract class MySQLSchemaTest extends SchemaTest {

  public abstract String getDBHost();
  public abstract String getDBUser();
  public abstract String getDBPassword();

  @Override
  public SQLConnection createConnection() {
    return new MySQLConnectionFactory().createConnection(getDBHost(), getDBUser(), getDBPassword());
  }
}
