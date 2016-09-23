package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.Series;
import com.mayhew3.gamesutil.model.tv.TVDBConnectionLog;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.JSONReader;
import com.mayhew3.gamesutil.xml.JSONReaderImpl;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class TVDBUpdateV2Runner {

  private enum SeriesUpdateResult {UPDATE_SUCCESS, UPDATE_FAILED}

  private Integer seriesUpdates = 0;
  private Integer episodesAdded = 0;
  private Integer episodesUpdated = 0;

  private SQLConnection connection;

  private TVDBJWTProvider tvdbjwtProvider;
  private JSONReader jsonReader;

  private TVDBConnectionLog tvdbConnectionLog;

  private final Integer ERROR_THRESHOLD = 3;
  private final Integer ERROR_FOLLOW_UP_THRESHOLD_IN_DAYS = 7;

  TVDBUpdateV2Runner(SQLConnection connection, TVDBJWTProvider tvdbjwtProvider, JSONReader jsonReader) {
    this.connection = connection;
    this.tvdbjwtProvider = tvdbjwtProvider;
    this.jsonReader = jsonReader;
  }

  public static void main(String... args) throws URISyntaxException, SQLException, UnirestException {
    List<String> argList = Lists.newArrayList(args);
    Boolean singleSeries = argList.contains("SingleSeries");
    Boolean quickMode = argList.contains("Quick");
    Boolean smartMode = argList.contains("Smart");
    Boolean recentlyUpdatedOnly = argList.contains("Recent");
    Boolean fewErrors = argList.contains("FewErrors");
    Boolean oldErrors = argList.contains("OldErrors");
    String identifier = new ArgumentChecker(args).getDBIdentifier();

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);
    TVDBUpdateV2Runner tvdbUpdateRunner = new TVDBUpdateV2Runner(connection, new TVDBJWTProviderImpl(), new JSONReaderImpl());

    if (singleSeries) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.SINGLE);
    } else if (quickMode) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.QUICK);
    } else if (smartMode) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.SMART);
    } else if (recentlyUpdatedOnly) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.RECENT);
    } else if (fewErrors) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.FEW_ERRORS);
    } else if (oldErrors) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.OLD_ERRORS);
    } else {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.FULL);
    }

    // update denorms after changes.
    new SeriesDenormUpdater(connection).updateFields();
  }

  public void runUpdate(@NotNull TVDBUpdateType updateType) throws SQLException, UnirestException {

    initializeConnectionLog(updateType);

    try {
      if (updateType.equals(TVDBUpdateType.FULL)) {
        runUpdate();
      } else if (updateType.equals(TVDBUpdateType.SMART)) {
        runSmartUpdate();
      } else if (updateType.equals(TVDBUpdateType.RECENT)) {
        runUpdateOnRecentUpdateList();
      } else if (updateType.equals(TVDBUpdateType.FEW_ERRORS)) {
        runUpdateOnRecentlyErrored();
      } else if (updateType.equals(TVDBUpdateType.OLD_ERRORS)) {
        runUpdateOnOldErrors();
      } else if (updateType.equals(TVDBUpdateType.SINGLE)) {
        runUpdateSingle();
      } else if (updateType.equals(TVDBUpdateType.QUICK)) {
        runQuickUpdate();
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
    tvdbConnectionLog.updateType.changeValue(updateType.getTypekey());
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
        "and ((tvdb_new = ? and last_tvdb_error is null and last_tvdb_update is null) " +
        "   or needs_tvdb_redo = ? or matched_wrong = ?) ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, true, true, true);

    runUpdateOnResultSet(resultSet);
  }



  private void runUpdateSingle() throws SQLException {
    String singleSeriesTitle = "The Good Place"; // update for testing on a single series

    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ? " +
        "and title = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, singleSeriesTitle);

    runUpdateOnResultSet(resultSet);
  }

  private void runSmartUpdate() throws SQLException, UnirestException {
    debug("");
    debug("-- STARTING UPDATE FOR TVDB RECENT UPDATE LIST -- ");
    debug("");

    runUpdateOnRecentUpdateList();

    debug("");
    debug("-- STARTING UPDATE FOR RECENTLY FAILED SHOWS WITH FEW ERRORS -- ");
    debug("");

    runUpdateOnRecentlyErrored();

    debug("");
    debug("-- STARTING UPDATE FOR OLD FAILED SHOWS WITH MANY ERRORS -- ");
    debug("");

    runUpdateOnOldErrors();

    debug("");
    debug("-- STARTING UPDATE FOR NEWLY ADDED SHOWS -- ");
    debug("");

    runQuickUpdate();

    debug("");
    debug("-- SMART UPDATE COMPLETE -- ");
    debug("");
  }

  private void runUpdateOnRecentUpdateList() throws UnirestException, SQLException {

    Timestamp mostRecentSuccessfulUpdate = getMostRecentSuccessfulUpdate();

    validateLastUpdate(mostRecentSuccessfulUpdate);

    JSONObject updatedSeries = tvdbjwtProvider.getUpdatedSeries(mostRecentSuccessfulUpdate);

    if (updatedSeries.isNull("data")) {
      debug("Empty list of TVDB updated.");
      return;
    }

    @NotNull JSONArray seriesArray = jsonReader.getArrayWithKey(updatedSeries, "data");

    for (int i = 0; i < seriesArray.length(); i++) {
      JSONObject seriesRow = seriesArray.getJSONObject(i);
      @NotNull Integer seriesId = jsonReader.getIntegerWithKey(seriesRow, "id");

      String sql = "select * " +
          "from series " +
          "where ignore_tvdb = ? " +
          "and tvdb_series_ext_id = ?";

      @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, seriesId);
      if (resultSet.next()) {
        Series series = new Series();

        try {
          SeriesUpdateResult updateResult = processSingleSeries(resultSet, series);
          if (SeriesUpdateResult.UPDATE_SUCCESS.equals(updateResult)) {
            tvdbConnectionLog.updatedShows.increment(1);
          } else {
            tvdbConnectionLog.failedShows.increment(1);
          }
        } catch (Exception e) {
          debug("Show failed on initialization from DB.");
        }
      } else {
        debug("Recently updated series not found: ID " + seriesId);
      }
    }
  }

  private void validateLastUpdate(Timestamp mostRecentSuccessfulUpdate) {
    DateTime mostRecent = new DateTime(mostRecentSuccessfulUpdate);
    DateTime sixDaysAgo = new DateTime().minusDays(6);
    if (mostRecent.isBefore(sixDaysAgo)) {
      throw new IllegalStateException("No updates in 6 days! Need to run a full update to catch up!");
    }
  }

  private void runUpdateOnRecentlyErrored() throws SQLException {
    String sql = "select *\n" +
        "from series\n" +
        "where last_tvdb_error is not null\n" +
        "and consecutive_tvdb_errors < ?\n" +
        "and ignore_tvdb = ?";

    @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, ERROR_THRESHOLD, false);
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
        "and consecutive_tvdb_errors >= ?\n" +
        "and ignore_tvdb = ?";

    @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, timestamp, ERROR_THRESHOLD, false);
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

      seriesUpdates++;
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

  private Timestamp getMostRecentSuccessfulUpdate() throws SQLException {
    String sql = "select max(start_time) as max_start_time\n" +
        "from tvdb_connection_log\n" +
        "where update_type in (?, ?, ?)\n" +
        "and finish_time is not null";
    @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql,
        TVDBUpdateType.FULL.getTypekey(),
        TVDBUpdateType.SMART.getTypekey(),
        TVDBUpdateType.RECENT.getTypekey()
    );
    if (resultSet.next()) {
      Timestamp maxStartTime = resultSet.getTimestamp("max_start_time");
      if (maxStartTime == null) {
        throw new IllegalStateException("Max start time should never be null.");
      } else {
        return maxStartTime;
      }
    } else {
      throw new IllegalStateException("Max start time should never be an empty set.");
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

  private void updateTVDB(Series series) throws SQLException, BadlyFormattedXMLException, ShowFailedException, UnirestException {
    TVDBSeriesV2Updater updater = new TVDBSeriesV2Updater(connection, series, tvdbjwtProvider, jsonReader);
    updater.updateSeries();

    episodesAdded += updater.getEpisodesAdded();
    episodesUpdated += updater.getEpisodesUpdated();
  }

  public Integer getSeriesUpdates() {
    return seriesUpdates;
  }

  public Integer getEpisodesAdded() {
    return episodesAdded;
  }

  public Integer getEpisodesUpdated() {
    return episodesUpdated;
  }



  protected void debug(Object object) {
    System.out.println(object);
  }

}

