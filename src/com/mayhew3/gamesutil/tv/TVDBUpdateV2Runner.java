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
import org.apache.http.auth.AuthenticationException;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@SuppressWarnings("Guava")
public class TVDBUpdateV2Runner {

  private enum SeriesUpdateResult {UPDATE_SUCCESS, UPDATE_FAILED}

  private Integer seriesUpdates = 0;
  private Integer episodesAdded = 0;
  private Integer episodesUpdated = 0;

  private SQLConnection connection;

  private TVDBJWTProvider tvdbjwtProvider;
  private JSONReader jsonReader;

  private TVDBConnectionLog tvdbConnectionLog;

  @SuppressWarnings("FieldCanBeLocal")
  private final Integer ERROR_FOLLOW_UP_THRESHOLD_IN_DAYS = 7;
  private final Integer ERROR_THRESHOLD = 5;

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
    Boolean updateOnlyAirTimes = argList.contains("AirTimes");
    Boolean sanityCheck = argList.contains("SanityCheck");
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
    } else if (updateOnlyAirTimes) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.AIRTIMES);
    } else if (sanityCheck) {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.SANITY);
    } else {
      tvdbUpdateRunner.runUpdate(TVDBUpdateType.FULL);
    }

    if (tvdbUpdateRunner.getSeriesUpdates() > 0) {
      // update denorms after changes.
      new SeriesDenormUpdater(connection).updateFields();
    }
  }

  public void runUpdate(@NotNull TVDBUpdateType updateType) throws SQLException, UnirestException {

    initializeConnectionLog(updateType);

    try {
      if (updateType.equals(TVDBUpdateType.FULL)) {
        runUpdate();
      } else if (updateType.equals(TVDBUpdateType.SMART)) {
        runSmartUpdateSingleQuery();
      } else if (updateType.equals(TVDBUpdateType.RECENT)) {
        runUpdateOnRecentUpdateList();
      } else if (updateType.equals(TVDBUpdateType.FEW_ERRORS)) {
        runUpdateOnRecentlyErrored();
      } else if (updateType.equals(TVDBUpdateType.OLD_ERRORS)) {
        runUpdateOnOldErrors();
      } else if (updateType.equals(TVDBUpdateType.SINGLE)) {
        runUpdateSingle();
      } else if (updateType.equals(TVDBUpdateType.AIRTIMES)) {
        runAirTimesUpdate();
      } else if (updateType.equals(TVDBUpdateType.QUICK)) {
        runQuickUpdate();
      } else if (updateType.equals(TVDBUpdateType.SANITY)) {
        runSanityUpdateOnShowsThatHaventBeenUpdatedInAWhile();
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
   * Go to theTVDB and update all matched series in my DB with the ones from theirs.
   *
   * @throws SQLException if query to get series to update fails. Any one series update will not halt operation of the
   *                    script, but if the query to find all the serieses fails, the operation can't continue.
   */
  public void runUpdate() throws SQLException {
    String sql = "select *\n" +
        "from series\n" +
        "where tvdb_match_status = ? " +
        "and retired = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, TVDBMatchStatus.MATCH_COMPLETED, 0);

    runUpdateOnResultSet(resultSet);
  }

  /**
   * Go to theTVDB and only update series that recently had their match confirmed by a user, but haven't yet updated their
   * episodes or series data.
   *
   * @throws SQLException if query to get series to update fails. Any one series update will not halt operation of the
   *                    script, but if the query to find all the serieses fails, the operation can't continue.
   */
  private void runQuickUpdate() throws SQLException {
    String sql = "select * " +
        "from series " +
        "where tvdb_match_status = ? " +
        "and consecutive_tvdb_errors < ? " +
        "and retired = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, TVDBMatchStatus.MATCH_CONFIRMED, ERROR_THRESHOLD, 0);

    runUpdateOnResultSet(resultSet);
  }

  // todo: run on shows that haven't been updated in the past week. Test first: queries to see how many this is.
  private void runSanityUpdateOnShowsThatHaventBeenUpdatedInAWhile() throws SQLException {
    DateTime today = new DateTime();
    Timestamp sevenDaysAgo = new Timestamp(today.minusDays(7).getMillis());
    Timestamp threeMonthsAgo = new Timestamp(today.minusMonths(3).getMillis());

    String sql = "select *\n" +
        "from series\n" +
        "where tvdb_match_status = ? " +
        "and last_tvdb_update is not null\n" +
        "and last_tvdb_update < ?\n" +
        "and suggestion = ?\n" +
        "and retired = ?\n" +
        "and most_recent > ?;";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, TVDBMatchStatus.MATCH_COMPLETED, sevenDaysAgo, false, 0, threeMonthsAgo);

    runUpdateOnResultSet(resultSet);
  }


  private void runUpdateSingle() throws SQLException {
    String singleSeriesTitle = "Unsolved Mysteries"; // update for testing on a single series

    String sql = "select *\n" +
        "from series\n" +
        "where title = ? " +
        "and retired = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, singleSeriesTitle, 0);

    runUpdateOnResultSet(resultSet);
  }

  private void runAirTimesUpdate() throws SQLException {
    String singleSeriesTitle = "Detroit Steel"; // update for testing on a single series

    String sql = "select *\n" +
        "from series\n" +
        "where title = ? " +
        "and retired = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, singleSeriesTitle, 0);

    while (resultSet.next()) {
      Series series = new Series();
      series.initializeFromDBObject(resultSet);

      debug("Updating series '" + series.seriesTitle.getValue() + "'");

      TVDBEpisodeV2Updater tvdbEpisodeV2Updater = new TVDBEpisodeV2Updater(series, connection, tvdbjwtProvider, 1, jsonReader, false);
      tvdbEpisodeV2Updater.updateOnlyAirTimes();
    }
  }


  private void runUpdateOnRecentUpdateList() throws UnirestException, SQLException, AuthenticationException {

    Timestamp mostRecentSuccessfulUpdate = getMostRecentSuccessfulUpdate();

    validateLastUpdate(mostRecentSuccessfulUpdate);

    debug("Finding all episodes updated since: " + mostRecentSuccessfulUpdate);

    JSONObject updatedSeries = tvdbjwtProvider.getUpdatedSeries(mostRecentSuccessfulUpdate);

    if (updatedSeries.isNull("data")) {
      debug("Empty list of TVDB updated.");
      return;
    }

    @NotNull JSONArray seriesArray = jsonReader.getArrayWithKey(updatedSeries, "data");

    debug("Total series found: " + seriesArray.length());

    for (int i = 0; i < seriesArray.length(); i++) {
      JSONObject seriesRow = seriesArray.getJSONObject(i);
      @NotNull Integer seriesId = jsonReader.getIntegerWithKey(seriesRow, "id");

      String sql = "select * " +
          "from series " +
          "where tvdb_match_status = ? " +
          "and tvdb_series_ext_id = ? " +
          "and retired = ? ";

      @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, TVDBMatchStatus.MATCH_COMPLETED, seriesId, 0);
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
        "and tvdb_match_status = ? " +
        "and retired = ? ";

    @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, ERROR_THRESHOLD, TVDBMatchStatus.MATCH_COMPLETED, 0);
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
        "and tvdb_match_status = ? " +
        "and retired = ? ";

    @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, timestamp, ERROR_THRESHOLD, TVDBMatchStatus.MATCH_COMPLETED, 0);
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

  private void runSmartUpdateSingleQuery() throws SQLException {

    debug("Starting update.");

    String sql = "select * " +
        "from series " +
        "where retired = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, 0);

    int i = 0;

    while (resultSet.next()) {
      i++;
      Series series = new Series();
      series.initializeFromDBObject(resultSet);

      if (shouldUpdateSeries(series)) {

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
    }

    debug("Update complete for series: " + i + " processed.");
  }

  private Boolean shouldUpdateSeries(Series series) {
    return isRecentlyErrored(series) ||
        isOldErrored(series) ||
        matchReadyToComplete(series) ||
        hasUnmatchedEpisodes(series);
  }

  private Boolean isRecentlyErrored(Series series) {
    return hasError(series) &&
        withinConsecutiveErrorThreshold(series) &&
        hasMatchStatus(series, TVDBMatchStatus.MATCH_COMPLETED);
  }

  private Boolean isOldErrored(Series series) {
    DateTime now = new DateTime(new Date());
    DateTime aWeekAgo = now.minusDays(ERROR_FOLLOW_UP_THRESHOLD_IN_DAYS);
    Timestamp timestamp = new Timestamp(aWeekAgo.toDate().getTime());

    return hasError(series) &&
        withinErrorDateThreshold(series, timestamp) &&
        hasMatchStatus(series, TVDBMatchStatus.MATCH_COMPLETED);
  }

  private Boolean matchReadyToComplete(Series series) {
    return hasMatchStatus(series, TVDBMatchStatus.MATCH_CONFIRMED) &&
        withinConsecutiveErrorThreshold(series);
  }



  private Boolean withinConsecutiveErrorThreshold(Series series) {
    return series.consecutiveTVDBErrors.getValue() < ERROR_THRESHOLD;
  }

  private Boolean hasMatchStatus(Series series, String matchStatus) {
    return matchStatus.equals(series.tvdbMatchStatus.getValue());
  }

  private Boolean hasError(Series series) {
    return series.lastTVDBError.getValue() != null;
  }

  private Boolean withinErrorDateThreshold(Series series, Timestamp timestamp) {
    return series.lastTVDBError.getValue().before(timestamp);
  }

  private Boolean hasUnmatchedEpisodes(Series series) {
    DateTime now = new DateTime(new Date());
    DateTime aDayAgo = now.minusDays(1);
    Timestamp timestamp = new Timestamp(aDayAgo.toDate().getTime());

    return series.lastTVDBUpdate.getValue() != null &&
        series.lastTVDBUpdate.getValue().before(timestamp) &&
        !series.isSuggestion.getValue() &&
        series.unmatchedEpisodes.getValue() > 0;
  }

  @NotNull
  private SeriesUpdateResult processSingleSeries(ResultSet resultSet, Series series) throws SQLException {
    if (!series.isInitialized()) {
      series.initializeFromDBObject(resultSet);
    }

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
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
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

  private void updateTVDB(Series series) throws SQLException, BadlyFormattedXMLException, ShowFailedException, UnirestException, AuthenticationException {
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



  protected void debug(Object message) {
    System.out.println(message);
  }

}

