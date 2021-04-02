package com.mayhew3.postgresobject.db;

public abstract class RemoteDatabaseEnvironment extends DatabaseEnvironment {

  final String remoteAppName;

  public RemoteDatabaseEnvironment(String environmentName, Integer pgVersion, String remoteAppName) {
    super(environmentName, pgVersion);
    this.remoteAppName = remoteAppName;
  }

  public RemoteDatabaseEnvironment(String environmentName, Integer pgVersion, String remoteAppName, String schemaName) {
    super(environmentName, schemaName, pgVersion);
    this.remoteAppName = remoteAppName;
  }

  public String getRemoteAppName() {
    return remoteAppName;
  }

  @Override
  public boolean isLocal() {
    return false;
  }
}
