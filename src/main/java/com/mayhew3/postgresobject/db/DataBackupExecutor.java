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

    // PGPASSFILE is only required for local environments
    // Remote environments use DATABASE_URL which includes credentials
    if (backupEnvironment.isLocal()) {
      String postgres_pgpass = EnvironmentChecker.getOrThrow("PGPASSFILE");
      File pgpass_file = new File(postgres_pgpass);
      assert pgpass_file.exists() && pgpass_file.isFile();
    }

    logger.info("Backing up from environment '" + backupEnvironment.getEnvironmentName() + "'");

    File postgres_program = new File(postgres_program_dir);
    assert postgres_program.exists() && postgres_program.isDirectory();

    File base_backup_dir = new File(backup_dir_location);
    assert base_backup_dir.exists() && base_backup_dir.isDirectory();

    File app_backup_dir = new File(backup_dir_location + File.separator + folderName);
    if (!app_backup_dir.exists()) {
      //noinspection ResultOfMethodCallIgnored
      app_backup_dir.mkdirs();
    }

    File env_backup_dir = new File(app_backup_dir.getPath() + File.separator + backupEnvironment.getEnvironmentName());
    if (!env_backup_dir.exists()) {
      //noinspection ResultOfMethodCallIgnored
      env_backup_dir.mkdirs();
    }

    File schema_backup_dir = backupEnvironment.getSchemaName() == null ? env_backup_dir :
        new File(env_backup_dir.getPath() + File.separator + backupEnvironment.getSchemaName());
    if (!schema_backup_dir.exists()) {
      //noinspection ResultOfMethodCallIgnored
      schema_backup_dir.mkdirs();
    }


    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    String formattedDate = dateFormat.format(new Date());

    String fullBackupPath = schema_backup_dir.getPath() + File.separator + formattedDate + ".dump";

    logger.info("Saving backup to file: " + fullBackupPath);

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
