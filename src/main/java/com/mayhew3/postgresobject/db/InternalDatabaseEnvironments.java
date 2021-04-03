package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.ArgumentChecker;

import java.util.HashMap;
import java.util.Map;

public class InternalDatabaseEnvironments {
  public static Map<String, DatabaseEnvironment> environments = new HashMap<>();

  public static LocalDatabaseEnvironment test = addLocal("test", "pg_object_test", 13);
  public static LocalDatabaseEnvironment testSchema = addLocal("test_schema", "pg_object_test_schema", "test_schema", 13);

  @SuppressWarnings("SameParameterValue")
  private static LocalDatabaseEnvironment addLocal(String environmentName, String databaseName, Integer pgVersion) {
    Integer port = 5432 - 9 + pgVersion;
    LocalDatabaseEnvironment local = new LocalDatabaseEnvironment(environmentName, databaseName, port, pgVersion);
    environments.put(environmentName, local);
    return local;
  }

  @SuppressWarnings("SameParameterValue")
  private static LocalDatabaseEnvironment addLocal(String environmentName, String databaseName, String schemaName, Integer pgVersion) {
    Integer port = 5432 - 9 + pgVersion;
    LocalDatabaseEnvironment local = new LocalDatabaseEnvironment(environmentName, databaseName, schemaName, port, pgVersion);
    environments.put(environmentName, local);
    return local;
  }

}
