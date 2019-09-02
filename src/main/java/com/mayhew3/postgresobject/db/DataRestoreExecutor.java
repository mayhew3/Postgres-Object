package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
public class DataRestoreExecutor {

  private String restoreEnv;
  private String backupEnv;
  private Optional<String> optionalAppName;
  private Integer pgVersion;
  private String folderName;
  private Optional<String> optionalDBName;

  private String aws_program_dir;
  private String aws_user_dir;
  private String heroku_program_dir;

  private String postgres_program_dir;
  private String postgres_pgpass_local;

  private Logger logger = LogManager.getLogger(DataRestoreExecutor.class);

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

  public DataRestoreExecutor(String restoreEnv, String backupEnv, Optional<String> optionalAppName, Integer pgVersion, String folderName, Optional<String> optionalDBName) {
    this.restoreEnv = restoreEnv;
    this.backupEnv = backupEnv;
    this.optionalAppName = optionalAppName;
    this.pgVersion = pgVersion;
    this.folderName = folderName;
    this.optionalDBName = optionalDBName;
  }

  public void runUpdate() throws MissingEnvException, IOException, InterruptedException {
    logger.info("Beginning execution of executor!");

    assert DataBackupExecutor.supportedVersions.contains(pgVersion);

    aws_program_dir = EnvironmentChecker.getOrThrow("AWS_PROGRAM_DIR");
    aws_user_dir = EnvironmentChecker.getOrThrow("AWS_USER_DIR");
    heroku_program_dir = EnvironmentChecker.getOrThrow("HEROKU_PROGRAM_DIR");

    String programEnvLabel = "POSTGRES" + pgVersion + "_PROGRAM_DIR";
    postgres_program_dir = EnvironmentChecker.getOrThrow(programEnvLabel);
    postgres_pgpass_local = EnvironmentChecker.getOrThrow("postgres_pgpass_local");
    String backup_dir_location = EnvironmentChecker.getOrThrow("DB_BACKUP_DIR");

    if (isLocal()) {
      assert optionalDBName.isPresent();
    } else {
      assert optionalAppName.isPresent();
      EnvironmentChecker.getOrThrow("DATABASE_URL");
    }

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

    // If no appName, do local restore. Otherwise restore to app.
    if (isLocal()) {
      logger.info("Restoring to local DB.");
      //noinspection OptionalGetWithoutIsPresent
      restoreToLocal(latestBackup, optionalDBName.get());
    } else {
      @SuppressWarnings("OptionalGetWithoutIsPresent")
      String appName = optionalAppName.get();
      logger.info("Restoring to Heroku app '" + appName + "'");
      String outputPath = getAWSPath(latestBackup);
      copyDBtoAWS(latestBackup, outputPath);
      String result = getSignedUrl(outputPath);
      restoreToHeroku(appName, result);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void restoreToLocal(Path latestBackup, String local_db_name) throws IOException, InterruptedException {

    ProcessBuilder processBuilder = new ProcessBuilder(
        postgres_program_dir + "\\pg_restore.exe",
        "--host=localhost",
        "--dbname=" + local_db_name,
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

  private void restoreToHeroku(String appName, String result) throws IOException, InterruptedException {

    ProcessBuilder processBuilder = new ProcessBuilder(
        heroku_program_dir + "\\heroku.cmd",
        "pg:backups:restore",
        "--app=" + appName,
        "\"" + result + "\"",
        "--confirm=" + appName,
        "DATABASE_URL"
    );

    processBuilder.inheritIO();

    logger.info("Starting db restore process...");

    Process process = processBuilder.start();
    process.waitFor();

    logger.info("Finished db restore process!");
  }

  private void copyDBtoAWS(Path latestBackup, String outputPath) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(
        aws_program_dir + "\\aws.exe",
        "s3",
        "cp",
        latestBackup.toString(),
        outputPath);

    processBuilder.inheritIO();

    logger.info("Starting db copy to aws...");

    Process process = processBuilder.start();
    process.waitFor();

    logger.info("Finished db copy process!");
  }

  @NotNull
  private String getAWSPath(Path latestBackup) {
    File aws_credentials = new File(aws_user_dir + "/credentials");
    assert aws_credentials.exists() && aws_credentials.isFile() :
        "Need to configure aws. See aws_info.txt";

    String bucketName = "s3://mediamogulbackups";
    return bucketName + "/" + latestBackup.getFileName();
  }

  @NotNull
  private String getSignedUrl(String outputPath) throws IOException {

    ProcessBuilder processBuilder = new ProcessBuilder(
        aws_program_dir + "\\aws.exe",
        "s3",
        "presign",
        outputPath
    );

    logger.info("Starting aws signed url generation...");

    Process process = processBuilder.start();

    BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream()));
    StringBuilder builder = new StringBuilder();
    String line;
    while ( (line = reader.readLine()) != null) {
      builder.append(line);
    }
    String result = builder.toString();

    assert !"".equals(result) : "No text found in AWS signed url!";

    logger.info("Finished aws signed url generation: ");
    logger.info("URL: " + result);
    return result;
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


  private Boolean isLocal() {
    return "local".equalsIgnoreCase(restoreEnv);
  }

}
