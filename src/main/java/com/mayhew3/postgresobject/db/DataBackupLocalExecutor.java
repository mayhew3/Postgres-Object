package com.mayhew3.postgresobject.db;

import java.io.IOException;

@SuppressWarnings("unused")
public class DataBackupLocalExecutor extends DataBackupExecutor {

  private final String localDBName;

  public DataBackupLocalExecutor(String backupEnv, Integer pgVersion, String folderName, String localDBName) {
    super(backupEnv, pgVersion, folderName);
    this.localDBName = localDBName;
  }

  @Override
  void executeBackup(String fullBackupPath) throws IOException, InterruptedException {
    int port = DataBackupExecutor.portMap.get(pgVersion);

    ProcessBuilder processBuilder = new ProcessBuilder(
        postgres_program_dir + "\\pg_dump.exe",
        "--host=localhost",
        "--port=" + port,
        "--dbname=" + localDBName,
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
