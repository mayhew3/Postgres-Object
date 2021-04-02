package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.exception.MissingEnvException;

@SuppressWarnings("unused")
public abstract class DatabaseEnvironment {

  final String environmentName;
  final String schemaName;
  final Integer pgVersion;

  public DatabaseEnvironment(String environmentName, Integer pgVersion) {
    this.environmentName = environmentName;
    this.schemaName = null;
    this.pgVersion = pgVersion;
  }

  public DatabaseEnvironment(String environmentName, String schemaName, Integer pgVersion) {
    this.environmentName = environmentName;
    this.schemaName = schemaName;
    this.pgVersion = pgVersion;
  }

  public String getEnvironmentName() {
    return environmentName;
  }
  public Integer getPgVersion() {
    return pgVersion;
  }
  public String getSchemaName() { return schemaName; }

  public abstract String getDatabaseUrl() throws MissingEnvException;
  public abstract boolean isLocal();
}
