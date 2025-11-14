package com.mayhew3.postgresobject.db;

import com.google.common.collect.Lists;
import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
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

    String maybeNullBackupSchemaName = backupEnvironment.getSchemaName();
    String backupSchemaName = maybeNullBackupSchemaName == null ? "public" : maybeNullBackupSchemaName;
    String restoreSchemaName = remoteDatabaseEnvironment.getSchemaName();

    logger.info("Restoring to Heroku app '" + appName + "'");
    String outputPath = getAWSPath(latestBackup);
    copyDBtoAWS(latestBackup, outputPath);
    String result = getSignedUrl(outputPath);

    List<String> args = Lists.newArrayList(heroku_program_dir + File.separator + getHerokuExecutable(),
        "pg:backups:restore",
        "--app=" + appName,
        "\"" + result + "\"",
        "--confirm=" + appName);

    if (restoreSchemaName != null) {
      dropSchema(restoreSchemaName);
      createSchema(backupSchemaName);
      args.add("--schema=" + backupSchemaName);
    }

    args.add("DATABASE_URL");

    ProcessBuilder processBuilder = new ProcessBuilder(args);

    processBuilder.environment().put("DATABASE_URL", databaseUrl);

    processBuilder.inheritIO();

    logger.info("Starting db restore process...");

    Process process = processBuilder.start();
    int exitCode = process.waitFor();

    if (exitCode != 0) {
      throw new IOException("Heroku pg:backups:restore process failed with exit code: " + exitCode);
    }

    logger.debug("Heroku restore completed successfully with exit code 0");
    logger.info("Finished db restore process!");
  }

  private void copyDBtoAWS(Path latestBackup, String outputPath) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(
        aws_program_dir + File.separator + getAwsExecutable(),
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
    File aws_credentials = new File(aws_user_dir + File.separator + "credentials");
    assert aws_credentials.exists() && aws_credentials.isFile() :
        "Need to configure aws. See aws_info.txt";

    String bucketName = "s3://mediamogulbackups";
    return bucketName + "/" + latestBackup.getFileName();
  }

  @NotNull
  private String getSignedUrl(String outputPath) throws IOException {

    ProcessBuilder processBuilder = new ProcessBuilder(
        aws_program_dir + File.separator + getAwsExecutable(),
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

  private void dropSchema(String restoreSchemaName) throws MissingEnvException {
    try {
      PostgresConnection connection = PostgresConnectionFactory.createConnection(remoteDatabaseEnvironment);
      connection.prepareAndExecuteStatementUpdate("DROP SCHEMA IF EXISTS " + restoreSchemaName + " CASCADE");
      connection.closeConnection();
    } catch (SQLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private void createSchema(String backupSchemaName) throws MissingEnvException {
    try {
      PostgresConnection connection = PostgresConnectionFactory.createConnection(remoteDatabaseEnvironment);
      connection.prepareAndExecuteStatementUpdate("CREATE SCHEMA IF NOT EXISTS " + backupSchemaName);
      connection.closeConnection();
    } catch (SQLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private void renameSchema(String restoreSchemaName, String backupSchemaName) throws MissingEnvException {
    try {
      PostgresConnection connection = PostgresConnectionFactory.createConnection(remoteDatabaseEnvironment);
      connection.prepareAndExecuteStatementUpdate("ALTER SCHEMA " + backupSchemaName + " RENAME TO " + restoreSchemaName);
      connection.closeConnection();
    } catch (SQLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
