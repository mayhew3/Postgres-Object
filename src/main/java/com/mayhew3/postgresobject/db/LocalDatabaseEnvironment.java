package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;

public class LocalDatabaseEnvironment extends DatabaseEnvironment {

  final String databaseName;
  final Integer port;

  public LocalDatabaseEnvironment(String environmentName, String databaseName, Integer port, Integer pgVersion) {
    super(environmentName, pgVersion);
    this.databaseName = databaseName;
    this.port = port;
  }

  public LocalDatabaseEnvironment(String environmentName, String databaseName, String schemaName, Integer port, Integer pgVersion) {
    super(environmentName, schemaName, pgVersion);
    this.databaseName = databaseName;
    this.port = port;
  }

  @Override
  public String getDatabaseUrl() throws MissingEnvException {
    String localPassword = EnvironmentChecker.getOrThrow("postgres_local_password");
    String schemaStr = this.schemaName == null ? "" : "&currentSchema=" + this.schemaName;
    return "jdbc:postgresql://localhost:" + port + "/" + databaseName + "?user=postgres&password=" + localPassword + schemaStr;
  }

  public String getDatabaseName() {
    return databaseName;
  }


  @Override
  public boolean isLocal() {
    return true;
  }
}
