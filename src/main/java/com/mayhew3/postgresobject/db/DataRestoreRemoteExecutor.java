package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

@SuppressWarnings("WeakerAccess")
public class DataRestoreRemoteExecutor extends DataRestoreExecutor {

  private final RemoteDatabaseEnvironment remoteDatabaseEnvironment;

  private String aws_program_dir;
  private String aws_user_dir;
  private String heroku_program_dir;

  public DataRestoreRemoteExecutor(RemoteDatabaseEnvironment restoreEnvironment, DatabaseEnvironment backupEnvironment, String folderName) throws MissingEnvException {
    super(restoreEnvironment, backupEnvironment, folderName);
    remoteDatabaseEnvironment = restoreEnvironment;
    checkEnvironment();
  }

  public DataRestoreRemoteExecutor(RemoteDatabaseEnvironment restoreEnvironment, DatabaseEnvironment backupEnvironment, String folderName, DateTime backupDate) throws MissingEnvException {
    super(restoreEnvironment, backupEnvironment, folderName, backupDate);
    remoteDatabaseEnvironment = restoreEnvironment;
    checkEnvironment();
  }

  private void checkEnvironment() throws MissingEnvException {
    aws_program_dir = EnvironmentChecker.getOrThrow("AWS_PROGRAM_DIR");
    aws_user_dir = EnvironmentChecker.getOrThrow("AWS_USER_DIR");
    heroku_program_dir = EnvironmentChecker.getOrThrow("HEROKU_PROGRAM_DIR");
  }

  @Override
  void executeRestore(Path latestBackup) throws IOException, InterruptedException, MissingEnvException {
    String appName = remoteDatabaseEnvironment.getRemoteAppName();
    String databaseUrl = remoteDatabaseEnvironment.getDatabaseUrl();

    logger.info("Restoring to Heroku app '" + appName + "'");
    String outputPath = getAWSPath(latestBackup);
    copyDBtoAWS(latestBackup, outputPath);
    String result = getSignedUrl(outputPath);

    ProcessBuilder processBuilder = new ProcessBuilder(
        heroku_program_dir + "\\heroku.cmd",
        "pg:backups:restore",
        "--app=" + appName,
        "\"" + result + "\"",
        "--confirm=" + appName,
        "DATABASE_URL"
    );
    processBuilder.environment().put("DATABASE_URL", databaseUrl);

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

}
