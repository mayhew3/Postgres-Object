package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.Series;
import com.mayhew3.gamesutil.model.tv.TVDBConnectionLog;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import org.apache.http.auth.AuthenticationException;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class TVDBSeriesV2MatchRunner {

  private enum SeriesUpdateResult {UPDATE_SUCCESS, UPDATE_FAILED}

  private SQLConnection connection;

  private TVDBJWTProvider tvdbjwtProvider;

  private TVDBConnectionLog tvdbConnectionLog;

  private final Integer ERROR_THRESHOLD = 3;

  @SuppressWarnings("FieldCanBeLocal")
  private final Integer ERROR_FOLLOW_UP_THRESHOLD_IN_DAYS = 7;

  private TVDBSeriesV2MatchRunner(SQLConnection connection, TVDBJWTProvider tvdbjwtProvider) {
    this.connection = connection;
    this.tvdbjwtProvider = tvdbjwtProvider;
  }

  public static void main(String... args) throws URISyntaxException, SQLException, UnirestException {
    List<String> argList = Lists.newArrayList(args);
    Boolean singleSeries = argList.contains("SingleSeries");
    Boolean smartMode = argList.contains("Smart");
    Boolean fewErrors = argList.contains("FewErrors");
    Boolean oldErrors = argList.contains("OldErrors");
    String identifier = new ArgumentChecker(args).getDBIdentifier();

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);
    TVDBSeriesV2MatchRunner tvdbUpdateRunner = new TVDBSeriesV2MatchRunner(connection, new TVDBJWTProviderImpl());

    if (singleSeries) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.SINGLE);
    } else if (smartMode) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.SMART);
    } else if (fewErrors) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.FEW_ERRORS);
    } else if (oldErrors) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.OLD_ERRORS);
    } else {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.FULL);
    }

  }

  public void runUpdate(@NotNull TVDBUpdateType updateType) throws SQLException, UnirestException {

    initializeConnectionLog(updateType);

    try {
      if (updateType.equals(TVDBUpdateType.FULL)) {
        runUpdate();
      } else if (updateType.equals(TVDBUpdateType.SMART)) {
        runSmartUpdate();
      } else if (updateType.equals(TVDBUpdateType.FEW_ERRORS)) {
        runUpdateOnRecentlyErrored();
      } else if (updateType.equals(TVDBUpdateType.OLD_ERRORS)) {
        runUpdateOnOldErrors();
      } else if (updateType.equals(TVDBUpdateType.SINGLE)) {
        runUpdateSingle();
      }

      tvdbConnectionLog.finishTime.changeValue(new Date());

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      tvdbConnectionLog.commit(connection);
      tvdbConnectionLog = null;
    }

  }

  private void initializeConnectionLog(@NotNull TVDBUpdateType updateType) {
    tvdbConnectionLog = new TVDBConnectionLog();
    tvdbConnectionLog.initializeForInsert();

    tvdbConnectionLog.startTime.changeValue(new Date());
    tvdbConnectionLog.updatedShows.changeValue(0);
    tvdbConnectionLog.failedShows.changeValue(0);
    tvdbConnectionLog.updateType.changeValue(updateType.getTypekey() + " (match only)");
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
        "where tvdb_match_status = ? " +
        "and last_tvdb_error is null ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, "Match First Pass");

    runUpdateOnResultSet(resultSet);
  }



  private void runUpdateSingle() throws SQLException {
    String singleSeriesTitle = "Sherlock on Masterpiece"; // update for testing on a single series

    String sql = "select *\n" +
        "from series\n" +
        "where tvdb_match_status = ? " +
        "and title = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, "Match First Pass", singleSeriesTitle);

    runUpdateOnResultSet(resultSet);
  }

  private void runSmartUpdate() throws SQLException, UnirestException {

    debug("");
    debug("-- STARTING UPDATE FOR NEEDS FIRST PASS SHOWS WITH NO ERRORS -- ");
    debug("");

    runUpdate();

    debug("");
    debug("-- STARTING UPDATE FOR RECENTLY FAILED SHOWS WITH FEW ERRORS -- ");
    debug("");

    runUpdateOnRecentlyErrored();

    debug("");
    debug("-- STARTING UPDATE FOR OLD FAILED SHOWS WITH MANY ERRORS -- ");
    debug("");

    runUpdateOnOldErrors();

  }

  private void runUpdateOnRecentlyErrored() throws SQLException {
    String sql = "select *\n" +
        "from series\n" +
        "where tvdb_match_status = ? " +
        "and last_tvdb_error is not null " +
        "and consecutive_tvdb_errors < ? ";

    @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, "Match First Pass", ERROR_THRESHOLD);
    runUpdateOnResultSet(resultSet);
  }

  private void runUpdateOnOldErrors() throws SQLException {
    DateTime now = new DateTime(new Date());
    DateTime aWeekAgo = now.minusDays(ERROR_FOLLOW_UP_THRESHOLD_IN_DAYS);
    Timestamp timestamp = new Timestamp(aWeekAgo.toDate().getTime());

    String sql = "select *\n" +
        "from series\n" +
        "where last_tvdb_error is not null\n" +
        "and last_tvdb_error < ?\n" +
        "and consecutive_tvdb_errors >= ? ";

    @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, timestamp, ERROR_THRESHOLD);
    runUpdateOnResultSet(resultSet);
  }


  private void runUpdateOnResultSet(ResultSet resultSet) throws SQLException {
    debug("Starting update.");

    int i = 0;

    while (resultSet.next()) {
      i++;
      Series series = new Series();

      try {
        @NotNull SeriesUpdateResult result = processSingleSeries(resultSet, series);
        if (result.equals(SeriesUpdateResult.UPDATE_SUCCESS)) {
          tvdbConnectionLog.updatedShows.increment(1);
        } else {
          tvdbConnectionLog.failedShows.increment(1);
        }
      } catch (Exception e) {
        debug("Show failed on initialization from DB.");
      }
    }

    debug("Update complete for result set: " + i + " processed.");
  }

  @NotNull
  private SeriesUpdateResult processSingleSeries(ResultSet resultSet, Series series) throws SQLException {
    series.initializeFromDBObject(resultSet);

    try {
      updateTVDB(series);
      resetTVDBErrors(series);
      return SeriesUpdateResult.UPDATE_SUCCESS;
    } catch (Exception e) {
      e.printStackTrace();
      debug("Show failed TVDB: " + series.seriesTitle.getValue());
      updateTVDBErrors(series);
      return SeriesUpdateResult.UPDATE_FAILED;
    }
  }

  private void updateTVDBErrors(Series series) throws SQLException {
    series.lastTVDBError.changeValue(new Date());
    series.consecutiveTVDBErrors.increment(1);
    series.commit(connection);
  }

  private void resetTVDBErrors(Series series) throws SQLException {
    series.lastTVDBError.changeValue(null);
    series.consecutiveTVDBErrors.changeValue(0);
    series.commit(connection);
  }

  private void updateTVDB(Series series) throws SQLException, BadlyFormattedXMLException, ShowFailedException, UnirestException, AuthenticationException {
    TVDBSeriesV2MatchUpdater updater = new TVDBSeriesV2MatchUpdater(connection, series, tvdbjwtProvider);
    updater.updateSeries();
  }


  protected void debug(Object message) {
    System.out.println(message);
  }

}

