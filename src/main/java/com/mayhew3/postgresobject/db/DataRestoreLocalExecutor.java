package com.mayhew3.postgresobject.db;

import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings("unused")
public class DataRestoreLocalExecutor extends DataRestoreExecutor {

  private final String localDBName;

  public DataRestoreLocalExecutor(String restoreEnv, String backupEnv, Integer pgVersion, String folderName, String localDBName) {
    super(restoreEnv, backupEnv, pgVersion, folderName);
    this.localDBName = localDBName;
  }

  @Override
  void executeRestore(Path latestBackup) throws IOException, InterruptedException {

    ProcessBuilder processBuilder = new ProcessBuilder(
        postgres_program_dir + "\\pg_restore.exe",
        "--host=localhost",
        "--dbname=" + localDBName,
        "--username=postgres",
        "--clean",
        "--format=custom",
        "--verbose",
        latestBackup.toString());
    processBuilder.environment().put("PGPASSFILE", postgres_pgpass_local);

    processBuilder.inheritIO();

    logger.info("Starting db restore process...");

    Process process = processBuilder.start();
    process.waitFor();

    logger.info("Finished db restore process!");

  }
}
