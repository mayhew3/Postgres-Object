package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.ArgumentChecker;

import java.util.HashMap;
import java.util.Map;

public class InternalDatabaseEnvironments {
  public static Map<String, DatabaseEnvironment> environments = new HashMap<>();

  static {
    addLocal("test", "pg_object_test", 13);
    addLocal("test_schema", "pg_object_test", "test_schema", 13);
  }

  @SuppressWarnings("SameParameterValue")
  private static void addLocal(String environmentName, String databaseName, Integer pgVersion) {
    Integer port = 5432 - 9 + pgVersion;
    LocalDatabaseEnvironment local = new LocalDatabaseEnvironment(environmentName, databaseName, port, pgVersion);
    environments.put(environmentName, local);
  }

  @SuppressWarnings("SameParameterValue")
  private static void addLocal(String environmentName, String databaseName, String schemaName, Integer pgVersion) {
    Integer port = 5432 - 9 + pgVersion;
    LocalDatabaseEnvironment local = new LocalDatabaseEnvironment(environmentName, databaseName, schemaName, port, pgVersion);
    environments.put(environmentName, local);
  }

  public static DatabaseEnvironment getEnvironmentForDBArgument(ArgumentChecker argumentChecker) {
    String dbIdentifier = argumentChecker.getDBIdentifier();
    DatabaseEnvironment databaseEnvironment = environments.get(dbIdentifier);
    if (databaseEnvironment == null) {
      throw new IllegalArgumentException("No environment found with name: " + dbIdentifier);
    }
    return databaseEnvironment;
  }
}
