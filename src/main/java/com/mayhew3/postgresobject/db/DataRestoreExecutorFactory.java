package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.joda.time.DateTime;

public class DataRestoreExecutorFactory {

  public static DataRestoreRemoteExecutor create(RemoteDatabaseEnvironment remoteDatabaseEnvironment, DatabaseEnvironment backupDatabaseEnvironment, String folderName) throws MissingEnvException {
    return new DataRestoreRemoteExecutor(remoteDatabaseEnvironment, backupDatabaseEnvironment, folderName);
  }

  public static DataRestoreLocalExecutor create(LocalDatabaseEnvironment remoteDatabaseEnvironment, DatabaseEnvironment backupDatabaseEnvironment, String folderName) {
    return new DataRestoreLocalExecutor(remoteDatabaseEnvironment, backupDatabaseEnvironment, folderName);
  }

  public static DataRestoreRemoteExecutor createOldRestore(RemoteDatabaseEnvironment remoteDatabaseEnvironment, DatabaseEnvironment backupDatabaseEnvironment, String folderName, DateTime backupDate) throws MissingEnvException {
    return new DataRestoreRemoteExecutor(remoteDatabaseEnvironment, backupDatabaseEnvironment, folderName, backupDate);
  }

  public static DataRestoreLocalExecutor createOldRestore(LocalDatabaseEnvironment remoteDatabaseEnvironment, DatabaseEnvironment backupDatabaseEnvironment, String folderName, DateTime backupDate) {
    return new DataRestoreLocalExecutor(remoteDatabaseEnvironment, backupDatabaseEnvironment, folderName, backupDate);
  }

}
