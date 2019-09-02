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
import java.util.Optional;

@SuppressWarnings({"WeakerAccess", "OptionalUsedAsFieldOrParameterType", "unused"})
public class DataBackupExecutor {

  public static final List<Integer> supportedVersions = Lists.newArrayList(9, 10);

  private Logger logger = LogManager.getLogger(DataBackupExecutor.class);
  
  private String backupEnv;
  private Integer pgVersion;
  private String folderName;
  private Optional<String> optionalDBName;

  private String postgres_program_dir;
  private String postgres_pgpass;

  public DataBackupExecutor(String backupEnv, Integer pgVersion, String folderName, Optional<String> optionalDBName) {
    this.backupEnv = backupEnv;
    this.pgVersion = pgVersion;
    this.folderName = folderName;
    this.optionalDBName = optionalDBName;
  }

  public void runUpdate() throws MissingEnvException, IOException, InterruptedException {
    logger.info("Beginning execution of executor!");

    assert DataBackupExecutor.supportedVersions.contains(pgVersion);

    String programEnvLabel = "POSTGRES" + pgVersion + "_PROGRAM_DIR";
    String db_url = null;
    postgres_program_dir = EnvironmentChecker.getOrThrow(programEnvLabel);
    String backup_dir_location = EnvironmentChecker.getOrThrow("DB_BACKUP_DIR");
    if (isLocal()) {
      assert optionalDBName.isPresent();
    } else {
      db_url = EnvironmentChecker.getOrThrow("DATABASE_URL");
    }
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

    if (isLocal()) {
      //noinspection OptionalGetWithoutIsPresent
      backupLocal(fullBackupPath, optionalDBName.get());
    } else {
      backupRemote(fullBackupPath, db_url);
    }

    logger.info("Finished db backup process!");
  }

  private void backupLocal(String fullBackupPath, String local_db_name) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(
        postgres_program_dir + "\\pg_dump.exe",
        "--host=localhost",
        "--dbname=" + local_db_name,
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

  private void backupRemote(String fullBackupPath, String db_url) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(
        postgres_program_dir + "\\pg_dump.exe",
        "--format=custom",
        "--verbose",
        "--file=" + fullBackupPath,
        "\"" + db_url + "\"");
    processBuilder.environment().put("PGPASSFILE", postgres_pgpass);

    processBuilder.inheritIO();

    logger.info("Starting db backup process...");

    Process process = processBuilder.start();
    process.waitFor();
  }

  private Boolean isLocal() {
    return "local".equalsIgnoreCase(backupEnv);
  }

}
