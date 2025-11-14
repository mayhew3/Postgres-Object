package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.exception.MissingEnvException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

@SuppressWarnings("unused")
public class DataBackupRemoteExecutor extends DataBackupExecutor {

  private final RemoteDatabaseEnvironment remoteDatabaseEnvironment;

  public DataBackupRemoteExecutor(RemoteDatabaseEnvironment backupEnvironment, String folderName) {
    super(backupEnvironment, folderName);
    this.remoteDatabaseEnvironment = backupEnvironment;
  }

  @Override
  void executeBackup(String fullBackupPath) throws IOException, InterruptedException, MissingEnvException, SQLException {
    String databaseUrl = remoteDatabaseEnvironment.getDatabaseUrl();

    ProcessBuilder processBuilder = new ProcessBuilder(
        postgres_program_dir + File.separator + getPgDumpExecutable(),
        "--format=custom",
        "--verbose",
        "--no-privileges",
        "--no-owner",
        "--file=" + fullBackupPath,
        databaseUrl);

    logger.info("Starting db backup process...");

    Process process = processBuilder.start();
    monitorOutput(process);
    int exitCode = process.waitFor();

    if (exitCode != 0) {
      throw new IOException("pg_dump process failed with exit code: " + exitCode);
    }

    logger.debug("pg_dump completed successfully with exit code 0");
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
