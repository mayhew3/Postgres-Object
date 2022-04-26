package com.mayhew3.postgresobject;

import com.mayhew3.postgresobject.db.*;
import com.mayhew3.postgresobject.exception.MissingEnvException;

import java.io.IOException;
import java.sql.SQLException;

public class SoftballBackupExecutor {

  private final DatabaseEnvironment databaseEnvironment;

  public static void main(String[] args) throws MissingEnvException, InterruptedException, IOException, SQLException {

    ArgumentChecker argumentChecker = new ArgumentChecker(args);
    argumentChecker.removeExpectedOption("db");
    argumentChecker.addExpectedOption("backupEnv", true, "Name of environment to backup (local, heroku, heroku-staging)");

    String backupEnv = argumentChecker.getRequiredValue("backupEnv");
    DatabaseEnvironment databaseEnvironment = InternalDatabaseEnvironments.environments.get(backupEnv);

    SoftballBackupExecutor softballBackupExecutor = new SoftballBackupExecutor(databaseEnvironment);
    softballBackupExecutor.runUpdate();
  }

  public SoftballBackupExecutor(DatabaseEnvironment databaseEnvironment) {
    this.databaseEnvironment = databaseEnvironment;
  }

  public void runUpdate() throws MissingEnvException, InterruptedException, IOException, SQLException {
    if (databaseEnvironment.isLocal()) {
      updateLocal();
    } else {
      updateRemote();
    }
  }

  private void updateLocal() throws MissingEnvException, InterruptedException, IOException, SQLException {
    LocalDatabaseEnvironment localDatabaseEnvironment = (LocalDatabaseEnvironment) databaseEnvironment;

    DataBackupExecutor executor = new DataBackupLocalExecutor(localDatabaseEnvironment, "Softball");
    executor.runUpdate();
  }

  private void updateRemote() throws MissingEnvException, IOException, InterruptedException, SQLException {
    HerokuDatabaseEnvironment herokuDatabaseEnvironment = (HerokuDatabaseEnvironment) databaseEnvironment;

    DataBackupExecutor executor = new DataBackupRemoteSchemaExecutor(herokuDatabaseEnvironment, "Softball");
    executor.runUpdate();
  }

}
