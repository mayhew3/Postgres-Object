package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.GlobalConstants;

import java.util.HashMap;
import java.util.Map;

public class InternalDatabaseEnvironments {
  public static Map<String, DatabaseEnvironment> environments = new HashMap<>();

  public static LocalDatabaseEnvironment test = addLocal("test", "projects", GlobalConstants.schemaName, GlobalConstants.postgresVersion);
  public static HerokuDatabaseEnvironment testStaging = addHeroku("test-staging", "postgresURL_staging", GlobalConstants.schemaName, GlobalConstants.postgresVersion, "test-schema");

  public static LocalDatabaseEnvironment softballLocal = addLocal("local-softball", "projects", "softball", GlobalConstants.postgresVersion);
  public static HerokuDatabaseEnvironment softballStaging = addHeroku("heroku-staging", "postgresURL_staging", GlobalConstants.schemaNameSoftball, GlobalConstants.postgresVersion, "honeybadger-softball");

  @SuppressWarnings("SameParameterValue")
  private static LocalDatabaseEnvironment addLocal(String environmentName, String databaseName, String schemaName, Integer pgVersion) {
    Integer port = 5432 - 9 + pgVersion;
    LocalDatabaseEnvironment local = new LocalDatabaseEnvironment(environmentName, databaseName, schemaName, port, pgVersion);
    environments.put(environmentName, local);
    return local;
  }

  @SuppressWarnings("SameParameterValue")
  private static HerokuDatabaseEnvironment addHeroku(String environmentName, String databaseName, String schemaName, Integer pgVersion, String herokuAppName) {
    HerokuDatabaseEnvironment remote = new HerokuDatabaseEnvironment(environmentName, databaseName, schemaName, pgVersion, herokuAppName);
    environments.put(environmentName, remote);
    return remote;
  }

}
