package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.dataobject.DataSchema;
import com.mayhew3.postgresobject.dataobject.MockMySQLSchema;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import com.mayhew3.postgresobject.model.MySQLSchemaTest;

public class MySQLTestTest extends MySQLSchemaTest {

  @Override
  public DataSchema getDataSchema() {
    return MockMySQLSchema.schema;
  }

  @Override
  public String getDBHost() {
    try {
      return EnvironmentChecker.getOrThrow("dbhost");
    } catch (MissingEnvException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String getDBUser() {
    try {
      return EnvironmentChecker.getOrThrow("dbuser");
    } catch (MissingEnvException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String getDBPassword() {
    try {
      return EnvironmentChecker.getOrThrow("dbpassword");
    } catch (MissingEnvException e) {
      throw new IllegalStateException(e);
    }
  }

}
