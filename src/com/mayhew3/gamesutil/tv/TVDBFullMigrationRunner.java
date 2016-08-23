package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.Series;
import com.mayhew3.gamesutil.model.tv.TVDBEpisode;
import com.mayhew3.gamesutil.model.tv.TVDBMigrationError;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.JSONReader;
import com.mayhew3.gamesutil.xml.JSONReaderImpl;
import com.mayhew3.gamesutil.xml.NodeReaderImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TVDBFullMigrationRunner {

  private SQLConnection connection;
  private TVDBJWTProvider tvdbjwtProvider;
  private JSONReader jsonReader;

  private TVDBFullMigrationRunner(SQLConnection connection) throws UnirestException {
    this.connection = connection;
    this.tvdbjwtProvider = new TVDBJWTProviderImpl();
    this.jsonReader = new JSONReaderImpl();
  }

  public static void main(String... args) throws URISyntaxException, SQLException, FileNotFoundException, UnirestException {
    List<String> argList = Lists.newArrayList(args);
    Boolean singleSeries = argList.contains("SingleSeries");
    Boolean quickMode = argList.contains("Quick");
    Boolean logToFile = argList.contains("LogToFile");
    String identifier = new ArgumentChecker(args).getDBIdentifier();

    if (logToFile) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
      String dateFormatted = simpleDateFormat.format(new Date());

      File file = new File("D:\\Projects\\mean_projects\\GamesDBUtil\\logs\\TVDBFullMigration_" + dateFormatted + ".log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);
      System.setOut(ps);
    }

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);
    TVDBFullMigrationRunner tvdbUpdateRunner = new TVDBFullMigrationRunner(connection);

    if (singleSeries) {
      tvdbUpdateRunner.runUpdateSingle();
    } else if (quickMode) {
      tvdbUpdateRunner.runQuickUpdate();
    } else {
      tvdbUpdateRunner.runUpdate();
    }

    tvdbUpdateRunner.checkUnmigratedEpisodes();

    // update denorms after changes.
    new SeriesDenormUpdater(connection).updateFields();
  }

  /**
   * Go to theTVDB and update all series in my DB with the ones from theirs.
   *
   * @throws SQLException if query to get series to update fails. Any one series update will not halt operation of the
   *                    script, but if the query to find all the serieses fails, the operation can't continue.
   */
  public void runUpdate() throws SQLException {
    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false);

    runUpdateOnResultSet(resultSet);
  }

  /**
   * Go to theTVDB and update new series.
   *
   * @throws SQLException if query to get series to update fails. Any one series update will not halt operation of the
   *                    script, but if the query to find all the serieses fails, the operation can't continue.
   */
  private void runQuickUpdate() throws SQLException {
    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ? " +
        "and (tvdb_new = ? or needs_tvdb_redo = ? or matched_wrong = ?) ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, true, true, true);

    runUpdateOnResultSet(resultSet);
  }



  private void runUpdateSingle() throws SQLException {
    String singleSeriesTitle = "Catastrophe"; // update for testing on a single series

    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ? " +
        "and title = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, singleSeriesTitle);

    runUpdateOnResultSet(resultSet);
  }

  private void checkUnmigratedEpisodes() throws SQLException {
    String sql = "select *\n" +
        "from tvdb_episode\n" +
        "where api_version = ?\n" +
//        "and series_name = 'It''s Always Sunny in Philadelphia'\n" +
        "and retired = ?";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, 1, 0);

    debug("Starting update of un-migrated episodes.");

    int i = 0;
    int retired = 0;

    while (resultSet.next()) {
      i++;

      TVDBEpisode tvdbEpisode = new TVDBEpisode();
      tvdbEpisode.initializeFromDBObject(resultSet);

      try {
        Series series = tvdbEpisode.getEpisode(connection).getSeries(connection);

        try {
          TVDBEpisodeV2Updater v2Updater = new TVDBEpisodeV2Updater(series,
              connection,
              tvdbjwtProvider,
              tvdbEpisode.tvdbEpisodeExtId.getValue(),
              jsonReader,
              true);
          TVDBEpisodeV2Updater.EPISODE_RESULT result = v2Updater.updateSingleEpisode();
          if (TVDBEpisodeV2Updater.EPISODE_RESULT.RETIRED.equals(result)) {
            retired++;
          }
        } catch (Exception e) {
          e.printStackTrace();
          addMigrationError(series, tvdbEpisode.id.getValue(), e);
        }

        debug(i + " processed.");

      } catch (ShowFailedException e) {
        e.printStackTrace();
      }

    }

    debug(retired + " of " + i + " rows retired.");
  }

  private void runUpdateOnResultSet(ResultSet resultSet) throws SQLException {
    debug("Starting update.");

    int i = 0;

    while (resultSet.next()) {
      i++;
      Series series = new Series();

      try {
        processSingleSeries(resultSet, series);
      } catch (Exception e) {
        debug("Show failed on initialization from DB.");
        addMigrationError(series, e);
      }

      debug(i + " processed.");
    }
  }

  private void addMigrationError(Series series, Exception e) throws SQLException {
    TVDBMigrationError migrationError = new TVDBMigrationError();
    migrationError.initializeForInsert();

    migrationError.seriesId.changeValue(series.id.getValue());
    migrationError.exceptionType.changeValue(e.getClass().toString());
    migrationError.exceptionMsg.changeValue(e.getMessage());

    migrationError.commit(connection);
  }

  private void addMigrationError(Series series, Integer tvdbEpisodeExtId, Exception e) throws SQLException {
    TVDBMigrationError migrationError = new TVDBMigrationError();
    migrationError.initializeForInsert();

    migrationError.seriesId.changeValue(series.id.getValue());
    migrationError.tvdbEpisodeExtId.changeValue(tvdbEpisodeExtId);
    migrationError.exceptionType.changeValue(e.getClass().toString());
    migrationError.exceptionMsg.changeValue(e.getMessage());

    migrationError.commit(connection);
  }


  private void processSingleSeries(ResultSet resultSet, Series series) throws SQLException {
    series.initializeFromDBObject(resultSet);

    try {
      updateTVDB(series);
    } catch (Exception e) {
      e.printStackTrace();
      debug("Show failed TVDB: " + series.seriesTitle.getValue());
    }
  }

  private void updateTVDB(Series series) throws SQLException, BadlyFormattedXMLException, ShowFailedException, UnirestException {
    TVDBSeriesUpdater updater = new TVDBSeriesUpdater(connection, series, new NodeReaderImpl(), new TVDBWebProvider());
    updater.updateSeries();

    TVDBSeriesV2Updater v2Updater = new TVDBSeriesV2Updater(connection, series, tvdbjwtProvider, jsonReader);
    v2Updater.updateSeries();
  }

  protected void debug(Object object) {
    System.out.println(object);
  }

}

