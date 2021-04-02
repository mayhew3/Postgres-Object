package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.ArgumentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;

import java.io.IOException;

public class GenericDataBackupExecutor {

  private final DatabaseEnvironment databaseEnvironment;

  public static void main(String[] args) throws MissingEnvException, InterruptedException, IOException {

    com.mayhew3.postgresobject.ArgumentChecker argumentChecker = new ArgumentChecker(args);
    argumentChecker.removeExpectedOption("db");
    argumentChecker.addExpectedOption("backupEnv", true, "Name of environment to backup (local, heroku, heroku-staging)");

    String backupEnv = argumentChecker.getRequiredValue("backupEnv");
    DatabaseEnvironment databaseEnvironment = InternalDatabaseEnvironments.environments.get(backupEnv);

    GenericDataBackupExecutor backupExecutor = new GenericDataBackupExecutor(databaseEnvironment);
    backupExecutor.runUpdate();
  }

  public GenericDataBackupExecutor(DatabaseEnvironment databaseEnvironment) {
    this.databaseEnvironment = databaseEnvironment;
  }

  public void runUpdate() throws MissingEnvException, InterruptedException, IOException {
    LocalDatabaseEnvironment localDatabaseEnvironment = (LocalDatabaseEnvironment) databaseEnvironment;

    DataBackupExecutor executor = new DataBackupLocalExecutor(localDatabaseEnvironment, "PostgresObject");
    executor.runUpdate();
  }

}
