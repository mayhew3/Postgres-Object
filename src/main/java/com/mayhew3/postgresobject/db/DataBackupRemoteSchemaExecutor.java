package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.exception.MissingEnvException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

@SuppressWarnings("unused")
public class DataBackupRemoteSchemaExecutor extends DataBackupExecutor {

  private final RemoteDatabaseEnvironment remoteDatabaseEnvironment;
  private final String schemaName;

  public DataBackupRemoteSchemaExecutor(RemoteDatabaseEnvironment backupEnvironment, String folderName, String schemaName) {
    super(backupEnvironment, folderName);
    this.remoteDatabaseEnvironment = backupEnvironment;
    this.schemaName = schemaName;
  }

  @Override
  void executeBackup(String fullBackupPath) throws IOException, InterruptedException, MissingEnvException, SQLException {
    String databaseUrl = remoteDatabaseEnvironment.getDatabaseUrl();

    ProcessBuilder processBuilder = new ProcessBuilder(
        postgres_program_dir + "\\pg_dump.exe",
        "--format=custom",
        "--verbose",
        "--no-privileges",
        "--no-owner",
        "--schema=" + schemaName,
        "--file=" + fullBackupPath,
        "\"" + databaseUrl + "\"");

    logger.info("Starting db backup process...");

    Process process = processBuilder.start();
    monitorOutput(process);
    process.waitFor();
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
}
