package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.exception.MissingEnvException;

import java.io.IOException;

@SuppressWarnings("unused")
public class DataBackupRemoteExecutor extends DataBackupExecutor {

  private final RemoteDatabaseEnvironment remoteDatabaseEnvironment;

  public DataBackupRemoteExecutor(RemoteDatabaseEnvironment backupEnvironment, String folderName) {
    super(backupEnvironment, folderName);
    this.remoteDatabaseEnvironment = backupEnvironment;
  }

  @Override
  void executeBackup(String fullBackupPath) throws IOException, InterruptedException, MissingEnvException {
    String databaseUrl = remoteDatabaseEnvironment.getDatabaseUrl();

    ProcessBuilder processBuilder = new ProcessBuilder(
        postgres_program_dir + "\\pg_dump.exe",
        "--format=custom",
        "--verbose",
        "--no-privileges",
        "--no-owner",
        "--file=" + fullBackupPath,
        "\"" + databaseUrl + "\"");
    processBuilder.environment().put("PGPASSFILE", postgres_pgpass);

    processBuilder.inheritIO();

    logger.info("Starting db backup process...");

    Process process = processBuilder.start();
    process.waitFor();
  }
}
