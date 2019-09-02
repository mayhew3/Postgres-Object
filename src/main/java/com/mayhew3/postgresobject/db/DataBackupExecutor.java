package com.mayhew3.postgresobject.db;

import com.google.common.collect.Lists;
import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
abstract public class DataBackupExecutor {

  public static final List<Integer> supportedVersions = Lists.newArrayList(9, 10);

  Logger logger = LogManager.getLogger(DataBackupExecutor.class);
  
  private String backupEnv;
  Integer pgVersion;
  private String folderName;

  String postgres_program_dir;
  String postgres_pgpass;

  public DataBackupExecutor(String backupEnv,
                            Integer pgVersion,
                            String folderName) {
    this.backupEnv = backupEnv;
    this.pgVersion = pgVersion;
    this.folderName = folderName;
  }

  public void runUpdate() throws MissingEnvException, IOException, InterruptedException {
    logger.info("Beginning execution of executor!");

    assert DataBackupExecutor.supportedVersions.contains(pgVersion);

    String programEnvLabel = "POSTGRES" + pgVersion + "_PROGRAM_DIR";
    postgres_program_dir = EnvironmentChecker.getOrThrow(programEnvLabel);
    String backup_dir_location = EnvironmentChecker.getOrThrow("DB_BACKUP_DIR");

    postgres_pgpass = EnvironmentChecker.getOrThrow("postgres_pgpass_local");

    logger.info("Backing up from environment '" + backupEnv + "'");

    File pgpass_file = new File(postgres_pgpass);
    assert pgpass_file.exists() && pgpass_file.isFile();

    File postgres_program = new File(postgres_program_dir);
    assert postgres_program.exists() && postgres_program.isDirectory();

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


    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
    String formattedDate = dateFormat.format(new Date());

    String fullBackupPath = env_backup_dir.getPath() + "\\" + formattedDate + ".dump";

    logger.info("Saving backup to file: " + fullBackupPath);

    executeBackup(fullBackupPath);

    logger.info("Finished db backup process!");
  }

  abstract void executeBackup(String fullBackupPath) throws IOException, InterruptedException;

}
