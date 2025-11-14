package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.ArgumentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.joda.time.DateTime;

import java.io.IOException;
import java.sql.SQLException;

public class GenericDataRestoreExecutor {

  private static final DateTime backupDate = new DateTime(2021, 3, 8, 20, 45, 0);

  private final DatabaseEnvironment backupEnvironment;
  private final DatabaseEnvironment restoreEnvironment;
  private final boolean oldBackup;

  @SuppressWarnings("FieldCanBeLocal")
  private final String appLabel = "PostgresObject";

  public GenericDataRestoreExecutor(DatabaseEnvironment backupEnvironment, DatabaseEnvironment restoreEnvironment, boolean oldBackup) {
    this.backupEnvironment = backupEnvironment;
    this.restoreEnvironment = restoreEnvironment;
    this.oldBackup = oldBackup;
  }

  public static void main(String... args) throws MissingEnvException, InterruptedException, IOException, SQLException {

    ArgumentChecker argumentChecker = new ArgumentChecker(args);
    argumentChecker.removeExpectedOption("db");
    argumentChecker.addExpectedOption("backupEnv", true, "Name of environment to backup (local, heroku, heroku-staging)");
    argumentChecker.addExpectedOption("restoreEnv", true, "Name of environment to restore (local, heroku, heroku-staging)");
    argumentChecker.addExpectedOption("oldBackup", true, "Name of environment to restore (local, heroku, heroku-staging)");

    String backupEnv = argumentChecker.getRequiredValue("backupEnv");
    String restoreEnv = argumentChecker.getRequiredValue("restoreEnv");

    boolean oldBackup = Boolean.parseBoolean(argumentChecker.getRequiredValue("oldBackup"));

    DatabaseEnvironment backupEnvironment = InternalDatabaseEnvironments.environments.get(backupEnv);
    DatabaseEnvironment restoreEnvironment = InternalDatabaseEnvironments.environments.get(restoreEnv);

    if (backupEnvironment == null) {
      throw new IllegalArgumentException("Invalid backupEnv: " + backupEnv);
    }
    if (restoreEnvironment == null) {
      throw new IllegalArgumentException("Invalid restoreEnv: " + restoreEnv);
    }

    GenericDataRestoreExecutor unchartedRestoreExecutor = new GenericDataRestoreExecutor(backupEnvironment, restoreEnvironment, oldBackup);
    unchartedRestoreExecutor.runUpdate();
  }

  public void runUpdate() throws InterruptedException, IOException, com.mayhew3.postgresobject.exception.MissingEnvException, SQLException {
    DataRestoreExecutor dataRestoreExecutor;

    if (restoreEnvironment.isLocal()) {
      LocalDatabaseEnvironment localRestoreEnvironment = (LocalDatabaseEnvironment) restoreEnvironment;
      if (oldBackup) {
        dataRestoreExecutor = new DataRestoreLocalExecutor(localRestoreEnvironment, backupEnvironment, appLabel, backupDate);
      } else {
        dataRestoreExecutor = new DataRestoreLocalExecutor(localRestoreEnvironment, backupEnvironment, appLabel);
      }
    } else {
      RemoteDatabaseEnvironment remoteRestoreEnvironment = (RemoteDatabaseEnvironment) restoreEnvironment;
      if (oldBackup) {
        dataRestoreExecutor = new DataRestoreRemoteExecutor(remoteRestoreEnvironment, backupEnvironment, appLabel, backupDate);
      } else {
        dataRestoreExecutor = new DataRestoreRemoteExecutor(remoteRestoreEnvironment, backupEnvironment, appLabel);
      }
    }

    dataRestoreExecutor.runUpdate();
  }

}
