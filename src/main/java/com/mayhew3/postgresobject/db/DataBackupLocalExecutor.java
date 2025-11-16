package com.mayhew3.postgresobject.db;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    String schemaName = localDatabaseEnvironment.getSchemaName();
    List<String> args = Lists.newArrayList(postgres_program_dir + File.separator + getPgDumpExecutable(),
        "--host=localhost",
        "--port=" + port,
        "--dbname=" + localDatabaseEnvironment.getDatabaseName(),
        "--username=postgres",
        "--format=custom",
        "--verbose",
        "--file=" + fullBackupPath);

    if (schemaName != null) {
      args.add("--schema=" + schemaName);
    }

    ProcessBuilder processBuilder = new ProcessBuilder(args);

    // Explicitly set PostgreSQL environment variables for authentication
    String pgpassfile = System.getenv("PGPASSFILE");
    if (pgpassfile != null) {
      processBuilder.environment().put("PGPASSFILE", pgpassfile);
      logger.debug("Set PGPASSFILE environment variable for pg_dump: " + pgpassfile);
    }

    String pgpassword = System.getenv("PGPASSWORD");
    if (pgpassword != null) {
      processBuilder.environment().put("PGPASSWORD", pgpassword);
      logger.debug("Set PGPASSWORD environment variable for pg_dump");
    }

    processBuilder.inheritIO();

    logger.info("Starting db backup process...");

    Process process = processBuilder.start();
    int exitCode = process.waitFor();

    if (exitCode != 0) {
      throw new IOException("pg_dump process failed with exit code: " + exitCode);
    }

    logger.debug("pg_dump completed successfully with exit code 0");
  }
}
