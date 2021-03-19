package com.mayhew3.postgresobject.db;

public class DataBackupExecutorFactory {

  public static DataBackupRemoteExecutor create(RemoteDatabaseEnvironment remoteDatabaseEnvironment, String folderName) {
    return new DataBackupRemoteExecutor(remoteDatabaseEnvironment, folderName);
  }

  public static DataBackupLocalExecutor create(LocalDatabaseEnvironment localDatabaseEnvironment, String folderName) {
    return new DataBackupLocalExecutor(localDatabaseEnvironment, folderName);
  }

}
