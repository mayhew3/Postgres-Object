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
      // Check CI environment variable first, fall back to local environment variable
      String host = System.getenv("MYSQL_HOST");
      if (host != null && !host.isEmpty()) {
        return host;
      }
      return EnvironmentChecker.getOrThrow("dbhost");
    } catch (MissingEnvException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String getDBUser() {
    try {
      // Check CI environment variable first, fall back to local environment variable
      String user = System.getenv("MYSQL_USER");
      if (user != null && !user.isEmpty()) {
        return user;
      }
      return EnvironmentChecker.getOrThrow("dbuser");
    } catch (MissingEnvException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String getDBPassword() {
    try {
      // Check CI environment variable first, fall back to local environment variable
      String password = System.getenv("MYSQL_PASSWORD");
      if (password != null && !password.isEmpty()) {
        return password;
      }
      return EnvironmentChecker.getOrThrow("dbpassword");
    } catch (MissingEnvException e) {
      throw new IllegalStateException(e);
    }
  }

}
