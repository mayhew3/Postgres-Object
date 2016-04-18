package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TiVoLibraryPostgresUpdater {

  public static void main(String[] args) throws FileNotFoundException, URISyntaxException, SQLException {
    List<String> argList = Lists.newArrayList(args);
    Boolean lookAtAllShows = argList.contains("FullMode");
    Boolean tvdbOnly = argList.contains("TVDBOnly");
    Boolean tiVoOnly = argList.contains("TiVoOnly");
    Boolean logToFile = argList.contains("LogToFile");
    Boolean runLocal = true;


    if (logToFile) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMM");
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

    SQLConnection connection = runLocal ? new PostgresConnectionFactory().createLocalConnection() :
        new PostgresConnectionFactory().createConnection();

    ConnectionPostgresLogger logger = new ConnectionPostgresLogger(connection);
    logger.initialize();

    logger.logConnectionStart(lookAtAllShows);

    if (!tvdbOnly) {
      try {
        TiVoCommunicatorPostgres tiVoCommunicator = new TiVoCommunicatorPostgres(connection);
        tiVoCommunicator.runUpdate(lookAtAllShows);
      } catch (BadlyFormattedXMLException e) {
        e.printStackTrace();
        debug("Error parsing TiVo XML.");
      } catch (SQLException e) {
        e.printStackTrace();
        debug("SQL error during TiVo update.");
      }
    }

    if (!tiVoOnly) {
      try {
        TVDBPostgresUpdateRunner tvdbUpdateRunner = new TVDBPostgresUpdateRunner(connection);
        tvdbUpdateRunner.runUpdate();
      } catch (SQLException e) {
        e.printStackTrace();
        debug("Error downloading info from TVDB service.");
      }
    }

    logger.logConnectionEnd();

    connection.closeConnection();
  }

  protected static void debug(Object object) {
    System.out.println(object);
  }

}
