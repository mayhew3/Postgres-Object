package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.Series;
import com.mayhew3.gamesutil.model.tv.TVDBWorkItem;
import com.mayhew3.gamesutil.xml.JSONReader;
import com.mayhew3.gamesutil.xml.JSONReaderImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class TVDBUpdateFinder {

  private SQLConnection connection;
  private TVDBJWTProvider tvdbjwtProvider;
  private JSONReader jsonReader;

  private Timestamp lastUpdated;

  private static PrintStream originalStream = System.out;

  private static Boolean logToFile = false;
  private static PrintStream logOutput = null;

  // TVDB doesn't seem to work right with a timestamp less than 120 seconds ago, always returns nothing.
  private Integer SECONDS = 120;

  public TVDBUpdateFinder(SQLConnection connection, TVDBJWTProvider tvdbjwtProvider, JSONReader jsonReader) {
    this.connection = connection;
    this.tvdbjwtProvider = tvdbjwtProvider;
    this.jsonReader = jsonReader;
  }

  public static void main(String... args) throws UnirestException, URISyntaxException, SQLException, InterruptedException, FileNotFoundException {
    List<String> argList = Lists.newArrayList(args);
    logToFile = argList.contains("LogToFile");

    if (logToFile) {
      openLogStream();
    }


    debug("");
    debug("SESSION START! Date: " + new Date());
    debug("");

    String identifier = new ArgumentChecker(args).getDBIdentifier();

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);
    TVDBUpdateFinder tvdbUpdateRunner = new TVDBUpdateFinder(connection, new TVDBJWTProviderImpl(), new JSONReaderImpl());

    tvdbUpdateRunner.runUpdater();
//    tvdbUpdateRunner.testUpdaterWorksSameWithPeriod(90);
  }

  private static void openLogStream() throws FileNotFoundException {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
    String dateFormatted = simpleDateFormat.format(new Date());

    File file = new File("D:\\Projects\\mean_projects\\GamesDBUtil\\logs\\TVDBUpdateFinder_" + dateFormatted + ".log");
    FileOutputStream fos = new FileOutputStream(file, true);
    logOutput = new PrintStream(fos);

    System.setErr(logOutput);
    System.setOut(logOutput);
  }

  private static void closeLogStream() {
    System.setErr(originalStream);
    System.setOut(originalStream);

    logOutput.close();
    logOutput = null;
  }

  private void runUpdater() throws SQLException, UnirestException, InterruptedException, FileNotFoundException {

    while (true) {
      if (logToFile && logOutput == null) {
        openLogStream();
      }

      debug(new Date());
      debug("Starting periodic update...");

      runPeriodicUpdate();

      debug("Finished run. Waiting " + SECONDS + " seconds...");

      if (logToFile && logOutput != null) {
        closeLogStream();
      }

      sleep(1000 * SECONDS);
    }
  }

  private void testUpdaterWorksSameWithPeriod(Integer seconds) throws SQLException, UnirestException, InterruptedException {
    DateTime now = DateTime.now();
    DateTime tenMinutesFromNow = now.plusMinutes(10);

    Timestamp veryFirstStartTime = getLastUpdateTime();

    if (veryFirstStartTime != null) {

      List<TVDBUpdate> updatesFromPeriodic = new ArrayList<>();

      while (tenMinutesFromNow.isAfterNow()) {
        debug("Starting periodic update...");

        updatesFromPeriodic.addAll(runPeriodicUpdate());

        debug("Finished run. Waiting " + seconds + " seconds...");
        sleep(1000 * seconds);
      }

      List<TVDBUpdate> updatesFromFull = runPeriodicUpdate(veryFirstStartTime);

      // going to assume there are some updates missing from the back end of each update. This test is going to focus
      // on no updates missed from between runs, and expect any missed from the most recent run will be made up for
      // in a successive run. Therefore, we use the final periodic update as a maximum, and make sure all the previous
      // ones are complete.
      Optional<Timestamp> maxUpdateTime = getMaxUpdateTime(updatesFromPeriodic);

      if (maxUpdateTime.isPresent()) {
        validatePeriodicUpdates(updatesFromPeriodic, updatesFromFull, maxUpdateTime.get());
      } else {
        debug("No periodic updates. Aborting comparison.");
      }
    } else {
      debug("No last update time. Aborting.");
    }
  }

  private void validatePeriodicUpdates(List<TVDBUpdate> updatesFromPeriodic, List<TVDBUpdate> updatesFromFull, Timestamp maxUpdateTime) {
    List<TVDBUpdate> eligibleUpdatesFromFull = updatesFromFull.stream()
        .filter(update -> !update.getTvdbLastUpdated().after(maxUpdateTime))
        .collect(Collectors.toList());

    List<TVDBUpdate> missedInPeriodic = Lists.newArrayList(eligibleUpdatesFromFull);
    missedInPeriodic.removeAll(updatesFromPeriodic);

    if (!missedInPeriodic.isEmpty()) {
      throw new RuntimeException("Episodes not found in periodic run: " + missedInPeriodic);
    }
  }

  private Optional<Timestamp> getMaxUpdateTime(List<TVDBUpdate> updates) {
    return updates.stream()
        .sorted(Comparator.comparing(TVDBUpdate::getTvdbLastUpdated).reversed())
        .map(TVDBUpdate::getTvdbLastUpdated)
        .findFirst();
  }

  private List<TVDBUpdate> runPeriodicUpdate() throws SQLException, UnirestException {
    Timestamp lastUpdateTime = getLastUpdateTime();
    if (lastUpdateTime != null) {
      Timestamp startTime = createStartTimeWithBuffer(lastUpdateTime);
      return runPeriodicUpdate(startTime);
    }
    return new ArrayList<>();
  }

  @NotNull
  private Timestamp createStartTimeWithBuffer(@NotNull Timestamp lastUpdateTime) {
    DateTime startTimeWithBuffer = new DateTime(lastUpdateTime).minusSeconds(120);
    return new Timestamp(startTimeWithBuffer.toDate().getTime());
  }

  @NotNull
  private List<TVDBUpdate> runPeriodicUpdate(@NotNull Timestamp startTime) throws UnirestException, SQLException {
    Timestamp now = now();

    Seconds secondsDiff = Seconds.secondsBetween(new DateTime(startTime), new DateTime(now));
    debug("Finding updates between " + startTime + " and " + now + ", diff of " + secondsDiff.getSeconds() + " seconds.");

    JSONObject updatedSeries = tvdbjwtProvider.getUpdatedSeries(startTime);


    if (updatedSeries.isNull("data")) {
      debug("Empty list of TVDB updated.");
      lastUpdated = now;
      return new ArrayList<>();
    }

    List<TVDBUpdate> allUpdates = processTVDBPayload(now, updatedSeries);

    lastUpdated = now;

    return allUpdates;
  }

  private List<TVDBUpdate> processTVDBPayload(Timestamp now, JSONObject updatedSeries) throws SQLException {
    @NotNull JSONArray seriesArray = jsonReader.getArrayWithKey(updatedSeries, "data");

    List<TVDBUpdate> allUpdates = new ArrayList<>();

    debug("Total series found: " + seriesArray.length());

    for (int i = 0; i < seriesArray.length(); i++) {
      JSONObject seriesRow = seriesArray.getJSONObject(i);
      @NotNull Integer tvdbSeriesExtId = jsonReader.getIntegerWithKey(seriesRow, "id");
      @NotNull Integer lastUpdatedEpoch = jsonReader.getIntegerWithKey(seriesRow, "lastUpdated");
      Timestamp tvdbLastUpdated = getFromEpochTime(lastUpdatedEpoch);

      allUpdates.add(new TVDBUpdate(tvdbSeriesExtId, tvdbLastUpdated));

      maybeUpdateSeries(now, tvdbSeriesExtId, tvdbLastUpdated);
    }

    return allUpdates;
  }

  private void maybeUpdateSeries(Timestamp now, Integer tvdbSeriesExtId, Timestamp tvdbLastUpdated) throws SQLException {
    String sql = "select * " +
        "from series " +
        "where ignore_tvdb = ? " +
        "and tvdb_series_ext_id = ?";

    @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, tvdbSeriesExtId);
    if (resultSet.next()) {
      if (hasWorkItemAlready(tvdbSeriesExtId, tvdbLastUpdated)) {
        debug("Work item already exists for TVDB ID " + tvdbSeriesExtId + " and update time of " + tvdbLastUpdated);
      } else {
        updateSingleSeries(now, tvdbSeriesExtId, tvdbLastUpdated, resultSet);
      }
    } else {
      debug("Recently updated series not found: ID " + tvdbSeriesExtId + ", updated " + tvdbLastUpdated);
    }
  }

  private Boolean hasWorkItemAlready(Integer tvdbSeriesExtId, Timestamp tvdbLastUpdated) throws SQLException {
    String sql = "select * " +
        "from tvdb_work_item " +
        "where tvdb_series_ext_id = ? " +
        "and last_updated = ?";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, tvdbSeriesExtId, tvdbLastUpdated);
    return resultSet.next();
  }

  private void updateSingleSeries(Timestamp now, Integer tvdbSeriesExtId, Timestamp tvdbLastUpdated, ResultSet resultSet) throws SQLException {
    Series series = new Series();
    series.initializeFromDBObject(resultSet);

    debug("Found update for series '" + series.seriesTitle.getValue() + "', updated " + tvdbLastUpdated);

    Integer seriesId = series.id.getValue();

    TVDBWorkItem workItem = new TVDBWorkItem();
    workItem.initializeForInsert();

    workItem.tvdbSeriesExtId.changeValue(tvdbSeriesExtId);
    workItem.seriesId.changeValue(seriesId);
    workItem.lastUpdated.changeValue(tvdbLastUpdated);
    workItem.foundTime.changeValue(now);

    workItem.commit(connection);
  }

  @NotNull
  private Timestamp now() {
    return new Timestamp(new Date().getTime());
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

  @Nullable
  private Timestamp getMostRecentPeriodicCheck() throws SQLException {
    String sql = "select max(found_time) as max_update_time " +
        "from tvdb_work_item ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql);
    if (resultSet.next()) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
      return resultSet.getTimestamp("max_update_time");
    }
    return null;
  }

  @Nullable
  private Timestamp getLastUpdateTime() throws SQLException {
    return lastUpdated == null ? getLastUpdateTimeFromDB() : lastUpdated;
  }

  @Nullable
  private Timestamp getLastUpdateTimeFromDB() throws SQLException {
    Timestamp mostRecentSuccessfulUpdate = getMostRecentSuccessfulUpdate();
    Timestamp mostRecentPeriodicCheck = getMostRecentPeriodicCheck();

    if (mostRecentPeriodicCheck == null) {
      return mostRecentSuccessfulUpdate;
    } else if (mostRecentSuccessfulUpdate == null) {
      return mostRecentPeriodicCheck;
    } else if (mostRecentPeriodicCheck.after(mostRecentSuccessfulUpdate)) {
      return mostRecentPeriodicCheck;
    } else {
      return mostRecentSuccessfulUpdate;
    }
  }

  private Timestamp getFromEpochTime(long epochTime) {
    return new Timestamp(epochTime * 1000L);
  }

  protected static void debug(Object message) {
    System.out.println(message);
  }

  private class TVDBUpdate {
    private Integer tvdbSeriesExtId;
    private Timestamp tvdbLastUpdated;

    private TVDBUpdate(Integer tvdbSeriesExtId, Timestamp tvdbLastUpdated) {
      this.tvdbSeriesExtId = tvdbSeriesExtId;
      this.tvdbLastUpdated = tvdbLastUpdated;
    }

    public Integer getTvdbSeriesExtId() {
      return tvdbSeriesExtId;
    }

    Timestamp getTvdbLastUpdated() {
      return tvdbLastUpdated;
    }

    @Override
    public String toString() {
      return "TVDB ID " + tvdbSeriesExtId + ", Updated " + tvdbLastUpdated;
    }

    @Override
    public boolean equals(Object obj) {
      assert obj instanceof TVDBUpdate;
      TVDBUpdate otherUpdate = (TVDBUpdate) obj;
      return otherUpdate.getTvdbSeriesExtId().equals(tvdbSeriesExtId) &&
          otherUpdate.getTvdbLastUpdated().equals(tvdbLastUpdated);
    }
  }
}
