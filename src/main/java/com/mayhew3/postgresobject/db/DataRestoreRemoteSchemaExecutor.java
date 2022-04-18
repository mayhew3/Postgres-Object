package com.mayhew3.postgresobject.db;

import com.google.common.collect.Lists;
import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public class DataRestoreRemoteSchemaExecutor extends DataRestoreExecutor {

  private final RemoteDatabaseEnvironment remoteDatabaseEnvironment;
  private final String schemaName;

  public DataRestoreRemoteSchemaExecutor(RemoteDatabaseEnvironment restoreEnvironment, DatabaseEnvironment backupEnvironment, String folderName, String schemaName) {
    super(restoreEnvironment, backupEnvironment, folderName);
    remoteDatabaseEnvironment = restoreEnvironment;
    this.schemaName = schemaName;
  }

  public DataRestoreRemoteSchemaExecutor(RemoteDatabaseEnvironment restoreEnvironment, DatabaseEnvironment backupEnvironment, String folderName, String schemaName, DateTime backupDate) {
    super(restoreEnvironment, backupEnvironment, folderName, backupDate);
    remoteDatabaseEnvironment = restoreEnvironment;
    this.schemaName = schemaName;
  }

  @Override
  void executeRestore(Path latestBackup) throws IOException, InterruptedException, MissingEnvException, SQLException {
    String postgres_pgpass_heroku = EnvironmentChecker.getOrThrow("postgres_pgpass_heroku");

    File pgpass_file = new File(postgres_pgpass_heroku);
    assert pgpass_file.exists() && pgpass_file.isFile();

    String appName = remoteDatabaseEnvironment.getRemoteAppName();
    String databaseUrl = remoteDatabaseEnvironment.getDatabaseUrl();

    String backupSchemaName = backupEnvironment.getSchemaName();
    String restoreSchemaName = remoteDatabaseEnvironment.getSchemaName();

    logger.info("Restoring to Heroku app '" + appName + "'");

    List<String> args = Lists.newArrayList(
        postgres_program_dir + "\\pg_restore.exe",
        "--dbname=" + databaseUrl,
        "--schema=" + backupSchemaName,
        latestBackup.toString());
/*

    dropSchema(restoreSchemaName);
    createSchema(backupSchemaName);
*/

    ProcessBuilder processBuilder = new ProcessBuilder(args);
    processBuilder.environment().put("PGPASSFILE", postgres_pgpass_heroku);

//    processBuilder.inheritIO();

    logger.info("Starting db restore process...");

    Process process = processBuilder.start();
    monitorOutput(process);
    process.waitFor();

    logger.info("Finished db restore process!");
  }

  private void monitorOutput(Process process) throws IOException, SQLException {
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getErrorStream()));
    StringBuilder builder = new StringBuilder();
    String line;
    while ( (line = reader.readLine()) != null) {
      if (line.contains("aborting")) {
        throw new SQLException("Backup process aborted: '" + line + "'");
      } else {
        System.err.println(line);
      }
    }
  }

  private void dropSchema(String restoreSchemaName) throws MissingEnvException {
    try {
      PostgresConnection connection = PostgresConnectionFactory.createConnection(remoteDatabaseEnvironment);
      connection.prepareAndExecuteStatementUpdate("DROP SCHEMA IF EXISTS " + restoreSchemaName + " CASCADE");
      connection.closeConnection();
    } catch (SQLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private void createSchema(String backupSchemaName) throws MissingEnvException {
    try {
      PostgresConnection connection = PostgresConnectionFactory.createConnection(remoteDatabaseEnvironment);
      connection.prepareAndExecuteStatementUpdate("CREATE SCHEMA IF NOT EXISTS " + backupSchemaName);
      connection.closeConnection();
    } catch (SQLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private void renameSchema(String restoreSchemaName, String backupSchemaName) throws MissingEnvException {
    try {
      PostgresConnection connection = PostgresConnectionFactory.createConnection(remoteDatabaseEnvironment);
      connection.prepareAndExecuteStatementUpdate("ALTER SCHEMA " + backupSchemaName + " RENAME TO " + restoreSchemaName);
      connection.closeConnection();
    } catch (SQLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
