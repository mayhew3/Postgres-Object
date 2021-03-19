package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;

import java.io.IOException;

public class GenericDataBackupExecutor {

  private static final DatabaseEnvironment backupEnv = DatabaseEnvironments.test;
  private static final String backupFolder = "PostgresObject";

  public static void main(String[] args) throws MissingEnvException, IOException, InterruptedException {
    if (backupEnv.isLocal()) {
      updateLocal();
    } else {
      updateRemote();
    }
  }

  private static void updateLocal() throws MissingEnvException, InterruptedException, IOException {
    DataBackupLocalExecutor executor = new DataBackupLocalExecutor((LocalDatabaseEnvironment) backupEnv, backupFolder);
    executor.runUpdate();
  }

  private static void updateRemote() throws MissingEnvException, IOException, InterruptedException {
    DataBackupRemoteExecutor executor = new DataBackupRemoteExecutor((RemoteDatabaseEnvironment) backupEnv, backupFolder);
    executor.runUpdate();
  }
}
