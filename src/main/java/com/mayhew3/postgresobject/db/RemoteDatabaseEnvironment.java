package com.mayhew3.postgresobject.db;

public abstract class RemoteDatabaseEnvironment extends DatabaseEnvironment {

  final String environmentVariableName;
  final String remoteAppName;

  public RemoteDatabaseEnvironment(String environmentName, String environmentVariableName, Integer pgVersion, String remoteAppName) {
    super(environmentName, pgVersion);
    this.environmentVariableName = environmentVariableName;
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
