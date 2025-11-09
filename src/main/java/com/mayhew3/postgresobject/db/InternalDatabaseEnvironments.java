package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.GlobalConstants;

import java.util.HashMap;
import java.util.Map;

public class InternalDatabaseEnvironments {
  public static Map<String, DatabaseEnvironment> environments = new HashMap<>();

  // Default to port 5439 for PostgreSQL 16, but allow override via POSTGRES_PORT environment variable
  private static final Integer DEFAULT_POSTGRES_PORT = 5439;
  private static final Integer DEFAULT_POSTGRES_VERSION = 16;

  public static LocalDatabaseEnvironment test = addLocal("test", "projects", GlobalConstants.schemaName, getPostgresPort(), getPostgresVersion());
  public static HerokuDatabaseEnvironment testStaging = addHeroku("test-staging", "postgresURL_staging", GlobalConstants.schemaName, getPostgresVersion(), "test-schema");

  public static LocalDatabaseEnvironment softballLocal = addLocal("local-softball", "projects", "softball", getPostgresPort(), getPostgresVersion());
  public static HerokuDatabaseEnvironment softballStaging = addHeroku("heroku-staging", "postgresURL_staging", GlobalConstants.schemaNameSoftball, getPostgresVersion(), "honeybadger-softball");

  /**
   * Get PostgreSQL port from environment variable POSTGRES_PORT, or use default 5439.
   */
  private static Integer getPostgresPort() {
    String portEnv = System.getenv("POSTGRES_PORT");
    return portEnv != null ? Integer.parseInt(portEnv) : DEFAULT_POSTGRES_PORT;
  }

  /**
   * Get PostgreSQL version from environment variable POSTGRES_VERSION, or use default 16.
   */
  private static Integer getPostgresVersion() {
    String versionEnv = System.getenv("POSTGRES_VERSION");
    return versionEnv != null ? Integer.parseInt(versionEnv) : DEFAULT_POSTGRES_VERSION;
  }

  @SuppressWarnings("SameParameterValue")
  private static LocalDatabaseEnvironment addLocal(String environmentName, String databaseName, String schemaName, Integer port, Integer pgVersion) {
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
