package com.mayhew3.postgresobject.db;

import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.file.Path;

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
  void executeRestore(Path latestBackup) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(
        postgres_program_dir + "\\pg_restore.exe",
        "--host=localhost",
        "--dbname=" + localRestoreEnvironment.getDatabaseName(),
        "--username=postgres",
        "--port=" + localRestoreEnvironment.port,
        "--no-privileges",
        "--no-owner",
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
