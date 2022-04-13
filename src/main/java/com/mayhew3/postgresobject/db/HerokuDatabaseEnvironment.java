package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;

public class HerokuDatabaseEnvironment extends RemoteDatabaseEnvironment {

  final String environmentVariableName;

  public HerokuDatabaseEnvironment(String environmentName, String environmentVariableName, Integer pgVersion, String herokuAppName) {
    super(environmentName, pgVersion, herokuAppName);
    this.environmentVariableName = environmentVariableName;
  }

  public HerokuDatabaseEnvironment(String environmentName, String environmentVariableName, String schemaName, Integer pgVersion, String herokuAppName) {
    super(environmentName, pgVersion, herokuAppName, schemaName);
    this.environmentVariableName = environmentVariableName;
  }

  @Override
  public String getDatabaseUrl() throws MissingEnvException {
    return EnvironmentChecker.getOrThrow(environmentVariableName);
  }

  @Override
  public boolean isLocal() {
    return false;
  }
}
