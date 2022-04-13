package com.mayhew3.postgresobject;

import com.mayhew3.postgresobject.db.*;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.joda.time.DateTime;

import java.io.IOException;
import java.sql.SQLException;

public class SoftballRestoreExecutor {

  private static final DateTime backupDate = new DateTime(2021, 3, 8, 20, 45, 0);

  private final DatabaseEnvironment backupEnvironment;
  private final DatabaseEnvironment restoreEnvironment;
  private final boolean oldBackup;

  public SoftballRestoreExecutor(DatabaseEnvironment backupEnvironment, DatabaseEnvironment restoreEnvironment, boolean oldBackup) {
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

    SoftballRestoreExecutor unchartedRestoreExecutor = new SoftballRestoreExecutor(backupEnvironment, restoreEnvironment, oldBackup);
    unchartedRestoreExecutor.runUpdate();
  }

  public void runUpdate() throws InterruptedException, IOException, MissingEnvException, SQLException {
    if (restoreEnvironment.isLocal()) {
      updateLocal();
    } else {
      updateRemote();
    }
  }

  private void updateLocal() throws MissingEnvException, InterruptedException, IOException, SQLException {
    LocalDatabaseEnvironment localRestoreEnvironment = (LocalDatabaseEnvironment) restoreEnvironment;

    DataRestoreExecutor dataRestoreExecutor;
    if (oldBackup) {
      dataRestoreExecutor = new DataRestoreLocalExecutor(localRestoreEnvironment, backupEnvironment, "Softball", backupDate);
    } else {
      dataRestoreExecutor = new DataRestoreLocalExecutor(localRestoreEnvironment, backupEnvironment, "Softball");
    }
    dataRestoreExecutor.runUpdate();

  }

  private void updateRemote() throws MissingEnvException, IOException, InterruptedException, SQLException {
    HerokuDatabaseEnvironment herokuRestoreEnvironment = (HerokuDatabaseEnvironment) restoreEnvironment;

    DataRestoreExecutor dataRestoreExecutor;
    if (oldBackup) {
      dataRestoreExecutor = new DataRestoreRemoteSchemaExecutor(herokuRestoreEnvironment, backupEnvironment, "Softball", "softball", backupDate);
    } else {
      dataRestoreExecutor = new DataRestoreRemoteSchemaExecutor(herokuRestoreEnvironment, backupEnvironment, "Softball", "softball");
    }
    dataRestoreExecutor.runUpdate();
  }

}
