package com.mayhew3.postgresobject.db;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.file.Path;
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
  void executeRestore(Path latestBackup) throws IOException, InterruptedException {
    String schemaName = localRestoreEnvironment.getSchemaName();

    List<String> args = Lists.newArrayList(postgres_program_dir + "\\pg_restore.exe",
        "--host=localhost",
        "--dbname=" + localRestoreEnvironment.getDatabaseName(),
        "--username=postgres",
        "--port=" + localRestoreEnvironment.port,
        "--no-privileges",
        "--no-owner",
        "--clean",
        "--format=custom",
        "--verbose");

    if (schemaName != null) {
      args.add("--schema=" + schemaName);
    }

    args.add(latestBackup.toString());

    ProcessBuilder processBuilder = new ProcessBuilder(args);
    processBuilder.environment().put("PGPASSFILE", postgres_pgpass_local);

    processBuilder.inheritIO();

    logger.info("Starting db restore process...");

    Process process = processBuilder.start();
    process.waitFor();

    logger.info("Finished db restore process!");

  }
}
