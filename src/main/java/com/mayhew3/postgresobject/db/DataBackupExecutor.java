package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

@SuppressWarnings({"WeakerAccess", "unused"})
abstract public class DataBackupExecutor {

  public static final Map<Integer, Integer> portMap = Map.ofEntries(
      new AbstractMap.SimpleEntry<>(9, 5432),
      new AbstractMap.SimpleEntry<>(10, 5433),
      new AbstractMap.SimpleEntry<>(11, 5434),
      new AbstractMap.SimpleEntry<>(12, 5435),
      new AbstractMap.SimpleEntry<>(13, 5436)
  );

  Logger logger = LogManager.getLogger(DataBackupExecutor.class);

  private final DatabaseEnvironment backupEnvironment;
  private final String folderName;

  String postgres_program_dir;

  public DataBackupExecutor(DatabaseEnvironment backupEnvironment, String folderName) {
    this.backupEnvironment = backupEnvironment;
    this.folderName = folderName;
  }

  public void runUpdate() throws MissingEnvException, IOException, InterruptedException, SQLException {
    logger.info("Beginning execution of executor!");

    String programEnvLabel = "POSTGRES" + backupEnvironment.getPgVersion() + "_PROGRAM_DIR";
    postgres_program_dir = EnvironmentChecker.getOrThrow(programEnvLabel);
    String backup_dir_location = EnvironmentChecker.getOrThrow("DB_BACKUP_DIR");

    logger.debug("PostgreSQL program directory: " + postgres_program_dir);
    logger.debug("Base backup directory location: " + backup_dir_location);
    logger.debug("Operating system: " + System.getProperty("os.name"));
    logger.debug("File separator: " + File.separator);

    // PGPASSFILE is only required for local environments
    // Remote environments use DATABASE_URL which includes credentials
    if (backupEnvironment.isLocal()) {
      String postgres_pgpass = EnvironmentChecker.getOrThrow("PGPASSFILE");
      File pgpass_file = new File(postgres_pgpass);
      assert pgpass_file.exists() && pgpass_file.isFile();
      logger.debug("Using PGPASSFILE: " + postgres_pgpass);
    } else {
      logger.debug("Skipping PGPASSFILE check for remote environment");
    }

    logger.info("Backing up from environment '" + backupEnvironment.getEnvironmentName() + "'");

    File postgres_program = new File(postgres_program_dir);
    assert postgres_program.exists() && postgres_program.isDirectory();

    File base_backup_dir = new File(backup_dir_location);
    assert base_backup_dir.exists() && base_backup_dir.isDirectory();

    File app_backup_dir = new File(backup_dir_location + File.separator + folderName);
    logger.debug("App backup directory: " + app_backup_dir.getPath());
    if (!app_backup_dir.exists()) {
      logger.debug("App backup directory does not exist, creating...");
      boolean created = app_backup_dir.mkdirs();
      logger.debug("App backup directory created: " + created);
    } else {
      logger.debug("App backup directory already exists");
    }

    File env_backup_dir = new File(app_backup_dir.getPath() + File.separator + backupEnvironment.getEnvironmentName());
    logger.debug("Environment backup directory: " + env_backup_dir.getPath());
    if (!env_backup_dir.exists()) {
      logger.debug("Environment backup directory does not exist, creating...");
      boolean created = env_backup_dir.mkdirs();
      logger.debug("Environment backup directory created: " + created);
    } else {
      logger.debug("Environment backup directory already exists");
    }

    File schema_backup_dir = backupEnvironment.getSchemaName() == null ? env_backup_dir :
        new File(env_backup_dir.getPath() + File.separator + backupEnvironment.getSchemaName());
    logger.debug("Schema backup directory: " + schema_backup_dir.getPath());
    if (!schema_backup_dir.exists()) {
      logger.debug("Schema backup directory does not exist, creating...");
      boolean created = schema_backup_dir.mkdirs();
      logger.debug("Schema backup directory created: " + created);
    } else {
      logger.debug("Schema backup directory already exists");
    }

    // Verify final directory exists
    if (!schema_backup_dir.exists()) {
      logger.error("CRITICAL: Schema backup directory does not exist after creation attempt: " + schema_backup_dir.getPath());
    } else {
      logger.debug("Schema backup directory verified to exist");
    }

    // Use UTC timezone with ISO 8601 format (matching Node.js backup executor)
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss-SSS'Z'");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    String formattedDate = dateFormat.format(new Date());

    // Prepend schema name to filename (e.g., mediamogul-2025-01-15T10-30-00-000Z.dump)
    String schemaPrefix = backupEnvironment.getSchemaName() != null ? backupEnvironment.getSchemaName() + "-" : "";
    String fullBackupPath = schema_backup_dir.getPath() + File.separator + schemaPrefix + formattedDate + ".dump";

    logger.info("Saving backup to file: " + fullBackupPath);
    logger.debug("Backup file parent directory exists: " + schema_backup_dir.exists());
    logger.debug("Backup file parent directory is writable: " + schema_backup_dir.canWrite());

    executeBackup(fullBackupPath);

    logger.info("Finished db backup process!");
  }

  abstract void executeBackup(String fullBackupPath) throws IOException, InterruptedException, MissingEnvException, SQLException;

  /**
   * Returns the appropriate pg_dump executable name for the current OS.
   * @return "pg_dump.exe" on Windows, "pg_dump" on Linux/Mac
   */
  protected String getPgDumpExecutable() {
    return isWindows() ? "pg_dump.exe" : "pg_dump";
  }

  /**
   * Returns the appropriate pg_restore executable name for the current OS.
   * @return "pg_restore.exe" on Windows, "pg_restore" on Linux/Mac
   */
  protected String getPgRestoreExecutable() {
    return isWindows() ? "pg_restore.exe" : "pg_restore";
  }

  /**
   * Detects if the current operating system is Windows.
   * @return true if running on Windows, false otherwise
   */
  protected boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }

}
