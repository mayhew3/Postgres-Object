package com.mayhew3.postgresobject.db;

import java.io.IOException;

@SuppressWarnings("unused")
public class DataBackupLocalExecutor extends DataBackupExecutor {

  private final LocalDatabaseEnvironment localDatabaseEnvironment;

  public DataBackupLocalExecutor(LocalDatabaseEnvironment backupEnvironment, String folderName) {
    super(backupEnvironment, folderName);
    this.localDatabaseEnvironment = backupEnvironment;
  }

  @Override
  void executeBackup(String fullBackupPath) throws IOException, InterruptedException {
    int port = localDatabaseEnvironment.port;

    ProcessBuilder processBuilder = new ProcessBuilder(
        postgres_program_dir + "\\pg_dump.exe",
        "--host=localhost",
        "--port=" + port,
        "--dbname=" + localDatabaseEnvironment.getDatabaseName(),
        "--username=postgres",
        "--format=custom",
        "--verbose",
        "--file=" + fullBackupPath);
    processBuilder.environment().put("PGPASSFILE", postgres_pgpass);

    processBuilder.inheritIO();

    logger.info("Starting db backup process...");

    Process process = processBuilder.start();
    process.waitFor();
  }
}
