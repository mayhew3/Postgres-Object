package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("WeakerAccess")
abstract public class DataRestoreExecutor {

  private String restoreEnv;
  private String backupEnv;
  Integer pgVersion;
  private String folderName;

  String postgres_program_dir;
  String postgres_pgpass_local;

  Logger logger = LogManager.getLogger(DataRestoreExecutor.class);

  private Comparator<Path> created = (file1, file2) -> {
    try {
      BasicFileAttributes attr1 = Files.readAttributes(file1, BasicFileAttributes.class);
      BasicFileAttributes attr2 = Files.readAttributes(file2, BasicFileAttributes.class);

      return attr2.creationTime().compareTo(attr1.creationTime());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  };

  public DataRestoreExecutor(String restoreEnv, String backupEnv, Integer pgVersion, String folderName) {
    this.restoreEnv = restoreEnv;
    this.backupEnv = backupEnv;
    this.pgVersion = pgVersion;
    this.folderName = folderName;
  }

  public void runUpdate() throws MissingEnvException, IOException, InterruptedException {
    logger.info("Beginning execution of executor: restoring '" + restoreEnv + "' from '" + backupEnv + "' backup");

    assert DataBackupExecutor.portMap.containsKey(pgVersion);

    String programEnvLabel = "POSTGRES" + pgVersion + "_PROGRAM_DIR";
    postgres_program_dir = EnvironmentChecker.getOrThrow(programEnvLabel);
    postgres_pgpass_local = EnvironmentChecker.getOrThrow("postgres_pgpass_local");
    String backup_dir_location = EnvironmentChecker.getOrThrow("DB_BACKUP_DIR");

    File pgpass_file = new File(postgres_pgpass_local);
    assert pgpass_file.exists() && pgpass_file.isFile();

    File postgres_program = new File(postgres_program_dir);
    assert postgres_program.exists() && postgres_program.isDirectory();

    File backup_dir = new File(backup_dir_location);
    assert backup_dir.exists() && backup_dir.isDirectory();

    File base_backup_dir = new File(backup_dir_location);
    assert base_backup_dir.exists() && base_backup_dir.isDirectory();

    File app_backup_dir = new File(backup_dir_location + "\\" + folderName);
    if (!app_backup_dir.exists()) {
      //noinspection ResultOfMethodCallIgnored
      app_backup_dir.mkdir();
    }

    File env_backup_dir = new File(app_backup_dir.getPath() + "\\" + backupEnv);
    if (!env_backup_dir.exists()) {
      //noinspection ResultOfMethodCallIgnored
      env_backup_dir.mkdir();
    }

    Path latestBackup = getLatestBackup(env_backup_dir.getPath());
    logger.info("File to restore: " + latestBackup.toString());

    executeRestore(latestBackup);
  }

  abstract void executeRestore(Path latestBackup) throws IOException, InterruptedException;

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

}
