package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;

import java.io.IOException;

public class GenericDataBackupExecutor {

  private static final String backupEnv = "local";
  private static final Integer pgVersion = 10;
  private static final String backupFolder = "OscarsAngular";
  private static final String localDBName = "oscars";

  public static void main(String[] args) throws MissingEnvException, IOException, InterruptedException {
    //noinspection ConstantConditions
    if ("local".equals(backupEnv)) {
      updateLocal();
    } else {
      updateRemote();
    }
  }

  private static void updateLocal() throws MissingEnvException, InterruptedException, IOException {
    DataBackupLocalExecutor executor = new DataBackupLocalExecutor(
        backupEnv,
        pgVersion,
        backupFolder,
        localDBName
    );
    executor.runUpdate();
  }

  private static void updateRemote() throws MissingEnvException, IOException, InterruptedException {
    String databaseUrl = EnvironmentChecker.getOrThrow("postgresURL_heroku");
    DataBackupRemoteExecutor executor = new DataBackupRemoteExecutor(
        backupEnv,
        pgVersion,
        backupFolder,
        databaseUrl);
    executor.runUpdate();
  }
}
