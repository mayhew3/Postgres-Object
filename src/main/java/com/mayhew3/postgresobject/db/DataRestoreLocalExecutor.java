package com.mayhew3.postgresobject.db;

import com.google.common.collect.Lists;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

@SuppressWarnings("unused")
public class DataRestoreLocalExecutor extends DataRestoreExecutor {

  private final LocalDatabaseEnvironment localRestoreEnvironment;

  public DataRestoreLocalExecutor(LocalDatabaseEnvironment restoreEnvironment, DatabaseEnvironment backupEnvironment, String folderName) {
    super(restoreEnvironment, backupEnvironment, folderName);
    this.localRestoreEnvironment = restoreEnvironment;
  }

  public DataRestoreLocalExecutor(LocalDatabaseEnvironment restoreEnvironment, DatabaseEnvironment backupEnvironment, String folderName, DateTime backupDate) {
    super(restoreEnvironment, backupEnvironment, folderName, backupDate);
    this.localRestoreEnvironment = restoreEnvironment;
  }

  @Override
  void executeRestore(Path latestBackup) throws IOException, InterruptedException, MissingEnvException {
    String maybeNullBackupSchemaName = backupEnvironment.getSchemaName();
    String backupSchemaName = maybeNullBackupSchemaName == null ? "public" : maybeNullBackupSchemaName;
    String restoreSchemaName = localRestoreEnvironment.getSchemaName();

    List<String> args = Lists.newArrayList(
        postgres_program_dir + "\\pg_restore.exe",
        "--host=localhost",
        "--dbname=" + localRestoreEnvironment.getDatabaseName(),
        "--username=postgres",
        "--port=" + localRestoreEnvironment.port,
        "--no-privileges",
        "--no-owner",
        "--clean",
        "--format=custom",
        "--verbose");

    if (restoreSchemaName != null) {
      dropSchema(restoreSchemaName);
      createSchema(backupSchemaName);
      args.add("--schema=" + backupSchemaName);
    }

    args.add(latestBackup.toString());

    ProcessBuilder processBuilder = new ProcessBuilder(args);
    processBuilder.environment().put("PGPASSFILE", postgres_pgpass_local);

    processBuilder.inheritIO();

    logger.info("Starting db restore process...");

    Process process = processBuilder.start();
    process.waitFor();

    logger.info("Finished db restore process!");

    if (restoreSchemaName != null && !restoreSchemaName.equals(backupSchemaName)) {
      renameSchema(restoreSchemaName, backupSchemaName);
    }

  }

  private void dropSchema(String restoreSchemaName) throws MissingEnvException {
    try {
      PostgresConnection connection = PostgresConnectionFactory.createConnection(localRestoreEnvironment);
      connection.prepareAndExecuteStatementUpdate("DROP SCHEMA IF EXISTS " + restoreSchemaName + " CASCADE");
      connection.closeConnection();
    } catch (SQLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private void createSchema(String backupSchemaName) throws MissingEnvException {
    try {
      PostgresConnection connection = PostgresConnectionFactory.createConnection(localRestoreEnvironment);
      connection.prepareAndExecuteStatementUpdate("CREATE SCHEMA IF NOT EXISTS " + backupSchemaName);
      connection.closeConnection();
    } catch (SQLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private void renameSchema(String restoreSchemaName, String backupSchemaName) throws MissingEnvException {
    try {
      PostgresConnection connection = PostgresConnectionFactory.createConnection(localRestoreEnvironment);
      connection.prepareAndExecuteStatementUpdate("ALTER SCHEMA " + backupSchemaName + " RENAME TO " + restoreSchemaName);
      connection.closeConnection();
    } catch (SQLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
