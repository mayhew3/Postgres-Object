package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.tv.helper.ConnectionLogger;
import com.mayhew3.gamesutil.tv.helper.UpdateMode;
import com.mayhew3.gamesutil.tv.provider.RemoteFileDownloader;
import com.mayhew3.gamesutil.tv.provider.TVDBJWTProviderImpl;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
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

public class TiVoLibraryUpdater {

  public static void main(String... args) throws FileNotFoundException, URISyntaxException, SQLException {
    List<String> argList = Lists.newArrayList(args);
    Boolean nightly = argList.contains("FullMode");
    Boolean tvdbOnly = argList.contains("TVDBOnly");
    Boolean tiVoOnly = argList.contains("TiVoOnly");
    Boolean logToFile = argList.contains("LogToFile");
    Boolean saveTiVoXML = argList.contains("SaveTiVoXML");

    String identifier = new ArgumentChecker(args).getDBIdentifier();
    TiVoCommunicator.UpdateType updateType = nightly ? TiVoCommunicator.UpdateType.FULL : TiVoCommunicator.UpdateType.QUICK;

    if (logToFile) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
      String dateFormatted = simpleDateFormat.format(new Date());

      String mediaMogulLogs = System.getenv("MediaMogulLogs");

      File file = new File(mediaMogulLogs + "\\TiVoUpdaterPostgres_" + dateFormatted + "_" + identifier + ".log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);
      System.setOut(ps);
    }

    debug("");
    debug("SESSION START! Date: " + new Date());
    debug("");

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);

    ConnectionLogger logger = new ConnectionLogger(connection);
    logger.initialize();

    logger.logConnectionStart(nightly);

    if (!tvdbOnly) {
      try {
        TiVoCommunicator tiVoCommunicator = new TiVoCommunicator(connection, new RemoteFileDownloader(saveTiVoXML), updateType);
        tiVoCommunicator.runUpdate();
      } catch (BadlyFormattedXMLException e) {
        debug("Error parsing TiVo XML.");
        e.printStackTrace();
      } catch (SQLException e) {
        debug("SQL error during TiVo update.");
        e.printStackTrace();
      }
    }

    if (!tiVoOnly) {
      try {
        TVDBUpdateRunner tvdbUpdateRunner = new TVDBUpdateRunner(connection, new TVDBJWTProviderImpl(), new JSONReaderImpl(), UpdateMode.SMART);
        tvdbUpdateRunner.runUpdate();
      } catch (SQLException e) {
        debug("Error downloading info from TVDB service.");
        e.printStackTrace();
      } catch (UnirestException e) {
        debug("Error initiating TVDB credentials.");
        e.printStackTrace();
      }
    }

    if (!tiVoOnly) {
      try {
        TVDBSeriesMatchRunner tvdbUpdateRunner = new TVDBSeriesMatchRunner(connection, new TVDBJWTProviderImpl(), new JSONReaderImpl(), UpdateMode.SMART);
        tvdbUpdateRunner.runUpdate();
      } catch (SQLException e) {
        debug("Error trying to match series with TVDB.");
        e.printStackTrace();
      } catch (UnirestException e) {
        debug("Error initiating TVDB credentials.");
        e.printStackTrace();
      }
    }

    if (nightly) {
      try {
        MetacriticTVUpdater metacriticTVUpdater = new MetacriticTVUpdater(connection);
        metacriticTVUpdater.runUpdate();
      } catch (Exception e) {
        debug("Uncaught exception during metacritic update.");
        e.printStackTrace();
      }
    }
    
    // update any shows that haven't been run in a while
    if (nightly) {
      try {
        TVDBUpdateRunner tvdbUpdateRunner = new TVDBUpdateRunner(connection, new TVDBJWTProviderImpl(), new JSONReaderImpl(), UpdateMode.SANITY);
        tvdbUpdateRunner.runUpdate();
      } catch (UnirestException e) {
        debug("Uncaught exception during TVDB sanity check.");
        e.printStackTrace();
      }
    }

    if (nightly) {
      try {
        debug("Updating EpisodeGroupRatings...");
        EpisodeGroupUpdater episodeGroupUpdater = new EpisodeGroupUpdater(connection);
        episodeGroupUpdater.runUpdate();
      } catch (Exception e) {
        debug("Uncaught exception during episode group rating update.");
        e.printStackTrace();
      }
    }

    try {
      debug("Updating denorms...");
      SeriesDenormUpdater denormUpdater = new SeriesDenormUpdater(connection);
      denormUpdater.runUpdate();
      debug("Denorms updated.");
    } catch (Exception e) {
      debug("Error updating series denorms.");
      e.printStackTrace();
    }

    logger.logConnectionEnd();

    connection.closeConnection();
  }

  protected static void debug(Object object) {
    System.out.println(object);
  }

}
