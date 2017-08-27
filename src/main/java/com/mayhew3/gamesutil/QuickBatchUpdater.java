package com.mayhew3.gamesutil;

import com.google.common.collect.Lists;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.games.SteamGameUpdater;
import com.mayhew3.gamesutil.scheduler.UpdateRunner;
import com.mayhew3.gamesutil.tv.*;
import com.mayhew3.gamesutil.xml.JSONReaderImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class QuickBatchUpdater {

  public static void main(String... args) throws FileNotFoundException, URISyntaxException, SQLException {
    List<String> argList = Lists.newArrayList(args);
    Boolean logToFile = argList.contains("LogToFile");
    Boolean saveTiVoXML = argList.contains("SaveTiVoXML");

    String identifier = new ArgumentChecker(args).getDBIdentifier();

    if (logToFile) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
      String dateFormatted = simpleDateFormat.format(new Date());

      String mediaMogulLogs = System.getenv("MediaMogulLogs");

      File file = new File(mediaMogulLogs + "\\QuickBatchUpdater_" + dateFormatted + "_" + identifier + ".log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);
      System.setOut(ps);
    }

    debug("");
    debug("SESSION START!");
    debug("");

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);


    // INITIALIZE UPDATERS

    List<UpdateRunner> updateRunners = Lists.newArrayList(
        new TiVoCommunicator(connection, new RemoteFileDownloader(saveTiVoXML), TiVoCommunicator.UpdateType.QUICK),
        new SteamGameUpdater(connection)
    );

    try {
      TVDBJWTProviderImpl tvdbjwtProvider = new TVDBJWTProviderImpl();
      updateRunners.add(new TVDBUpdateRunner(connection, tvdbjwtProvider, new JSONReaderImpl(), TVDBUpdateType.SMART));
      updateRunners.add(new TVDBSeriesMatchRunner(connection, new TVDBJWTProviderImpl(), new JSONReaderImpl(), TVDBUpdateType.SMART));
    } catch (UnirestException e) {
      debug("Error getting TVDB token. Skipping TVDB updates.");
      e.printStackTrace();
    }

    updateRunners.add(new SeriesDenormUpdater(connection));


    // RUN UPDATERS

    ConnectionLogger logger = new ConnectionLogger(connection);
    logger.initialize();

    logger.logConnectionStart(false);

    for (UpdateRunner updateRunner : updateRunners) {
      String runnerName = updateRunner.getRunnerName();
      debug("Beginning execution of updater '" + runnerName + "'");

      try {
        updateRunner.runUpdate();
        debug("Finished execution of updater '" + runnerName + "'");
      } catch (Exception e) {
        debug("Error during update: " + runnerName);
        e.printStackTrace();
      }
    }

    logger.logConnectionEnd();

    connection.closeConnection();
  }

  protected static void debug(Object object) {
    System.out.println(new Date() + ": " + object);
  }

}
