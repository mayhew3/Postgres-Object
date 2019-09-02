package com.mayhew3.postgresobject.db;

import java.io.IOException;

@SuppressWarnings("unused")
public class DataBackupRemoteExecutor extends DataBackupExecutor {

  private final String databaseUrl;

  public DataBackupRemoteExecutor(String backupEnv, Integer pgVersion, String folderName, String databaseUrl) {
    super(backupEnv, pgVersion, folderName);
    this.databaseUrl = databaseUrl;
  }

  @Override
  void executeBackup(String fullBackupPath) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(
        postgres_program_dir + "\\pg_dump.exe",
        "--format=custom",
        "--verbose",
        "--file=" + fullBackupPath,
        "\"" + databaseUrl + "\"");
    processBuilder.environment().put("PGPASSFILE", postgres_pgpass);

    processBuilder.inheritIO();

    logger.info("Starting db backup process...");

    Process process = processBuilder.start();
    process.waitFor();
  }
}
