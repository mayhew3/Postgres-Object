package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("WeakerAccess")
abstract public class DataRestoreExecutor {

  private final DatabaseEnvironment restoreEnvironment;
  final DatabaseEnvironment backupEnvironment;

  private final String folderName;
  private DateTime backupDate;

  String postgres_program_dir;

  Logger logger = LogManager.getLogger(DataRestoreExecutor.class);

  private final Comparator<Path> created = (file1, file2) -> {
    try {
      BasicFileAttributes attr1 = Files.readAttributes(file1, BasicFileAttributes.class);
      BasicFileAttributes attr2 = Files.readAttributes(file2, BasicFileAttributes.class);

      return attr2.creationTime().compareTo(attr1.creationTime());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  };

  public DataRestoreExecutor(DatabaseEnvironment restoreEnvironment, DatabaseEnvironment backupEnvironment, String folderName) {
    this.restoreEnvironment = restoreEnvironment;
    this.backupEnvironment = backupEnvironment;
    this.folderName = folderName;
  }

  public DataRestoreExecutor(DatabaseEnvironment restoreEnvironment, DatabaseEnvironment backupEnvironment, String folderName, DateTime backupDate) {
    this(restoreEnvironment, backupEnvironment, folderName);
    this.backupDate = backupDate;
  }

  public void runUpdate() throws MissingEnvException, IOException, InterruptedException, SQLException {
    logger.info("Beginning execution of executor: restoring '" + restoreEnvironment.getEnvironmentName() + "' from '" + backupEnvironment.getEnvironmentName() + "' backup");

    // PGPASSFILE is only required for local environments
    // Remote environments use DATABASE_URL which includes credentials
    if (restoreEnvironment.isLocal()) {
      String postgres_pgpass = EnvironmentChecker.getOrThrow("PGPASSFILE");
      File pgpass_file = new File(postgres_pgpass);
      assert pgpass_file.exists() && pgpass_file.isFile();
    }

    String programEnvLabel = "POSTGRES" + restoreEnvironment.getPgVersion() + "_PROGRAM_DIR";
    postgres_program_dir = EnvironmentChecker.getOrThrow(programEnvLabel);
    String backup_dir_location = EnvironmentChecker.getOrThrow("DB_BACKUP_DIR");

    File postgres_program = new File(postgres_program_dir);
    assert postgres_program.exists() && postgres_program.isDirectory();

    File backup_dir = new File(backup_dir_location);
    assert backup_dir.exists() && backup_dir.isDirectory();

    File base_backup_dir = new File(backup_dir_location);
    assert base_backup_dir.exists() && base_backup_dir.isDirectory();

    File app_backup_dir = new File(backup_dir_location + File.separator + folderName);
    File env_backup_dir = new File(app_backup_dir.getPath() + File.separator + backupEnvironment.getEnvironmentName());
    File schema_backup_dir = backupEnvironment.getSchemaName() == null ? env_backup_dir :
        new File(env_backup_dir.getPath() + File.separator + backupEnvironment.getSchemaName());

    Path latestBackup = getBackup(schema_backup_dir.getPath());
    logger.info("File to restore: " + latestBackup.toString());

    executeRestore(latestBackup);
  }

  abstract void executeRestore(Path latestBackup) throws IOException, InterruptedException, MissingEnvException, SQLException;

  private Path getBackup(String backup_directory) throws IOException {
    if (backupDate == null) {
      return getLatestBackup(backup_directory);
    } else {
      return getBackupForDate(backup_directory);
    }
  }

  private Path getBackupForDate(String backup_directory) throws IOException {
    Path path = Paths.get(backup_directory);
    List<Path> files = new ArrayList<>();

    logger.info("Finding newest backup before date: " + backupDate);

    DirectoryStream<Path> paths = Files.newDirectoryStream(path);
    for (Path path1 : paths) {
      File file = new File(path1.toString());
      if (file.isFile() && fileCreatedBeforeDate(path1, backupDate)) {
        files.add(path1);
      }
    }
    if (files.isEmpty()) {
      throw new IllegalStateException("No files found before date: " + backupDate + " in directory: " + backup_directory);
    }
    files.sort(created);
    return files.get(0);
  }

  private boolean fileCreatedBeforeDate(Path path, DateTime comparisonDate) throws IOException {
    BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
    Instant createTime = attributes.creationTime().toInstant();
    Instant backupTime = comparisonDate.toDate().toInstant();
    return createTime.isBefore(backupTime);
  }

  private Path getLatestBackup(String backup_directory) throws IOException {
    Path path = Paths.get(backup_directory);
    List<Path> files = new ArrayList<>();
    DirectoryStream<Path> paths = Files.newDirectoryStream(path);
    for (Path path1 : paths) {
      File file = new File(path1.toString());
      if (file.isFile()) {
        files.add(path1);
      }
    }
    files.sort(created);
    return files.get(0);
  }

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
   * Returns the appropriate AWS CLI executable name for the current OS.
   * @return "aws.exe" on Windows, "aws" on Linux/Mac
   */
  protected String getAwsExecutable() {
    return isWindows() ? "aws.exe" : "aws";
  }

  /**
   * Returns the appropriate Heroku CLI executable name for the current OS.
   * @return "heroku.cmd" on Windows, "heroku" on Linux/Mac
   */
  protected String getHerokuExecutable() {
    return isWindows() ? "heroku.cmd" : "heroku";
  }

  /**
   * Detects if the current operating system is Windows.
   * @return true if running on Windows, false otherwise
   */
  protected boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }

}
