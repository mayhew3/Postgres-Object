package com.mayhew3.postgresobject.db;

import java.util.HashMap;
import java.util.Map;

public class InternalDatabaseEnvironments {
  public static Map<String, DatabaseEnvironment> environments = new HashMap<>();

  public static LocalDatabaseEnvironment test = addLocal("test", "pg_object_test", 13);
  public static LocalDatabaseEnvironment testSchema = addLocal("test_schema", "pg_object_test_schema", "test_schema", 13);
  public static HerokuDatabaseEnvironment staging = addHeroku("heroku-staging", "postgresURL_softball", "softball", 13, "honeybadger-softball");

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

  @SuppressWarnings("SameParameterValue")
  private static HerokuDatabaseEnvironment addHeroku(String environmentName, String databaseName, Integer pgVersion, String herokuAppName) {
    HerokuDatabaseEnvironment remote = new HerokuDatabaseEnvironment(environmentName, databaseName, pgVersion, herokuAppName);
    environments.put(environmentName, remote);
    return remote;
  }

  @SuppressWarnings("SameParameterValue")
  private static HerokuDatabaseEnvironment addHeroku(String environmentName, String databaseName, String schemaName, Integer pgVersion, String herokuAppName) {
    HerokuDatabaseEnvironment remote = new HerokuDatabaseEnvironment(environmentName, databaseName, schemaName, pgVersion, herokuAppName);
    environments.put(environmentName, remote);
    return remote;
  }

}
