package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;

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
    Boolean lookAtAllShows = argList.contains("FullMode");
    Boolean tvdbOnly = argList.contains("TVDBOnly");
    Boolean tiVoOnly = argList.contains("TiVoOnly");
    Boolean logToFile = argList.contains("LogToFile");
    Boolean saveTiVoXML = argList.contains("SaveTiVoXML");

    String identifier = new ArgumentChecker(args).getDBIdentifier();

    if (logToFile) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
      String dateFormatted = simpleDateFormat.format(new Date());

      File file = new File("D:\\Projects\\mean_projects\\GamesDBUtil\\logs\\TiVoUpdaterPostgres_" + dateFormatted + ".log");
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

    logger.logConnectionStart(lookAtAllShows);

    if (!tvdbOnly) {
      try {
        TiVoCommunicator tiVoCommunicator = new TiVoCommunicator(connection, saveTiVoXML);
        tiVoCommunicator.runUpdate(lookAtAllShows);
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
        TVDBUpdateV2Runner tvdbUpdateRunner = new TVDBUpdateV2Runner(connection);
        tvdbUpdateRunner.runUpdate();
      } catch (SQLException e) {
        debug("Error downloading info from TVDB service.");
        e.printStackTrace();
      }
    }

    if (!tiVoOnly && !tvdbOnly) {
      try {
        MetacriticTVUpdater metacriticTVUpdater = new MetacriticTVUpdater(connection);
        metacriticTVUpdater.runUpdater();
      } catch (Exception e) {
        debug("Uncaught exception during metacritic update.");
        e.printStackTrace();
      }
    }

    try {
      debug("Updating denorms...");
      SeriesDenormUpdater denormUpdater = new SeriesDenormUpdater(connection);
      denormUpdater.updateFields();
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
