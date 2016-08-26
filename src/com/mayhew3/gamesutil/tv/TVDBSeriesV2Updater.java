package com.mayhew3.gamesutil.tv;

import com.google.common.base.Joiner;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.gamesutil.dataobject.FieldValue;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.*;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.JSONReader;
import com.mayhew3.gamesutil.xml.JSONReaderImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TVDBSeriesV2Updater {

  private Series series;

  private SQLConnection connection;
  private TVDBJWTProvider tvdbDataProvider;
  private JSONReader jsonReader;

  private Integer _episodesAdded = 0;
  private Integer _episodesUpdated = 0;
  private Integer episodesFailed = 0;

  public TVDBSeriesV2Updater(SQLConnection connection,
                             @NotNull Series series,
                             TVDBJWTProvider tvdbWebProvider,
                             JSONReader jsonReader) {
    this.series = series;
    this.connection = connection;
    this.tvdbDataProvider = tvdbWebProvider;
    this.jsonReader = jsonReader;
  }


  void updateSeries() throws SQLException, ShowFailedException, UnirestException, BadlyFormattedXMLException {
    String seriesTitle = series.seriesTitle.getValue();
    String seriesTiVoId = series.tivoSeriesExtId.getValue();

    ErrorLog errorLog = getErrorLog(seriesTiVoId);

    if (shouldIgnoreShow(errorLog)) {
      markSeriesToIgnore(series);
      resolveError(errorLog);
    } else {

      Boolean matchedWrong = series.matchedWrong.getValue();
      Integer existingId = series.tvdbSeriesExtId.getValue();

      Integer tvdbId = getTVDBID(errorLog, matchedWrong, existingId);

      Boolean usingOldWrongID = matchedWrong && Objects.equals(existingId, tvdbId);

      if (tvdbId != null && !usingOldWrongID) {
        debug(seriesTitle + ": ID found, getting show data.");
        series.tvdbSeriesExtId.changeValue(tvdbId);

        if (matchedWrong || series.needsTVDBRedo.getValue()) {
          Integer seriesId = series.id.getValue();
          unlinkAndRemoveEpisodes(seriesId);
          series.needsTVDBRedo.changeValue(false);
          series.matchedWrong.changeValue(false);
        }

        updateShowData(series);

        if (series.tivoSeriesExtId.getValue() != null) {
          tryToMatchUnmatchedEpisodes(series);
        }
      }
    }
  }

  // todo: never return null, just throw exception if failed to find
  @Nullable
  private Integer getTVDBID(@Nullable ErrorLog errorLog, Boolean matchedWrong, Integer existingId) throws SQLException, ShowFailedException, UnirestException {
    if (existingId != null && !matchedWrong) {
      return existingId;
    }

    try {
      return findTVDBMatch(series, errorLog);
    } catch (IOException | SAXException e) {
      e.printStackTrace();
      // todo: add error log
      throw new ShowFailedException("Error downloading XML from TVDB.");
    }
  }

  private void tryToMatchUnmatchedEpisodes(Series series) throws SQLException {
    List<TiVoEpisode> unmatchedEpisodes = findUnmatchedEpisodes(series);

    debug(unmatchedEpisodes.size() + " unmatched episodes found.");

    List<String> newlyMatched = new ArrayList<>();

    for (TiVoEpisode tivoEpisode : unmatchedEpisodes) {
      TVDBEpisodeMatcher matcher = new TVDBEpisodeMatcher(connection, tivoEpisode, series.id.getValue());
      TVDBEpisode tvdbEpisode = matcher.findTVDBEpisodeMatch();

      if (tvdbEpisode != null) {
        Episode episode = tvdbEpisode.getEpisode(connection);
        episode.addToTiVoEpisodes(connection, tivoEpisode);

        newlyMatched.add(episode.getSeason() + "x" + episode.episodeNumber.getValue());
      }
    }

    if (!newlyMatched.isEmpty()) {
      String join = Joiner.on(", ").join(newlyMatched);
      debug(newlyMatched.size() + " episodes matched: " + join);
    }
  }

  private List<TiVoEpisode> findUnmatchedEpisodes(Series series) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT te.* " +
            "FROM tivo_episode te " +
            "WHERE te.tivo_series_ext_id = ? " +
            "AND NOT EXISTS (SELECT 1 " +
                            "FROM edge_tivo_episode ete " +
                            "WHERE ete.tivo_episode_id = te.id) " +
            "ORDER BY te.episode_number, te.showing_start_time",
        series.tivoSeriesExtId.getValue()
    );
    List<TiVoEpisode> tiVoEpisodes = new ArrayList<>();
    while (resultSet.next()) {
      TiVoEpisode tiVoEpisode = new TiVoEpisode();
      tiVoEpisode.initializeFromDBObject(resultSet);
      tiVoEpisodes.add(tiVoEpisode);
    }
    return tiVoEpisodes;
  }

  private void unlinkAndRemoveEpisodes(Integer seriesId) throws SQLException {

    connection.prepareAndExecuteStatementUpdate(
        "DELETE FROM edge_tivo_episode " +
            "WHERE episode_id IN (SELECT id " +
            "                     FROM episode " +
            "                     WHERE series_id = ?)", seriesId
    );

    connection.prepareAndExecuteStatementUpdate(
        "UPDATE episode " +
            "SET on_tivo = ?, retired = id " +
            "WHERE series_id = ?", false, seriesId
    );

    connection.prepareAndExecuteStatementUpdate(
        "UPDATE tvdb_episode " +
            "SET retired = id " +
            "WHERE id IN (SELECT tvdb_episode_id " +
            "           FROM episode " +
            "           WHERE series_id = ?)",
        seriesId
    );

  }

  @Nullable
  private ErrorLog getErrorLog(String tivoId) throws SQLException {
    if (tivoId == null) {
      return null;
    }
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT *" +
            "FROM error_log " +
            "WHERE tivo_id = ? " +
            "AND resolved = ?", tivoId, false
    );
    if (resultSet.next()) {
      ErrorLog errorLog = new ErrorLog();
      errorLog.initializeFromDBObject(resultSet);
      return errorLog;
    }

    return null;
  }

  @Nullable
  private Integer findTVDBMatch(Series series, @Nullable ErrorLog errorLog) throws SQLException, IOException, SAXException, UnirestException {
    String seriesTitle = series.seriesTitle.getValue();
    String tivoId = series.tivoSeriesExtId.getValue();
    String tvdbHint = series.tvdbHint.getValue();

    String titleToCheck = (tvdbHint == null || "".equals(tvdbHint)) ?
        getTitleToCheck(seriesTitle, errorLog) :
        tvdbHint;

    if (titleToCheck == null) {
      throw new RuntimeException("Title to check is null. TiVoSeriesId: " + tivoId);
    }

    String formattedTitle = titleToCheck
        .toLowerCase()
        .replaceAll(" ", "_");

    debug("Update for: " + seriesTitle + ", formatted as '" + formattedTitle + "'");

    JSONObject seriesMatches = tvdbDataProvider.findSeriesMatches(formattedTitle);
    JSONArray seriesNodes = seriesMatches.getJSONArray("data");

    if (seriesNodes.length() == 0) {
      debug("Show not found!");
      if (!isNotFoundError(errorLog)) {
        addShowNotFoundErrorLog(series, formattedTitle, "Empty result found.");
      }
      return null;
    }

    if (isNotFoundError(errorLog)) {
      resolveError(errorLog);
    }

    JSONObject firstSeries = seriesNodes.getJSONObject(0);
    String seriesName = firstSeries.getString("seriesName");

    attachPossibleSeries(series, seriesNodes);

    if (!seriesTitle.equalsIgnoreCase(seriesName) && !titleToCheck.equalsIgnoreCase(seriesName)) {
      if (shouldAcceptMismatch(errorLog)) {
        updateSeriesTitle(series, errorLog);
      } else {
        debug("Discrepency between TiVo and TVDB names!");

        if (!isMismatchError(errorLog)) {
          addMismatchErrorLog(tivoId, seriesTitle, formattedTitle, seriesName);
        }
        return null;
      }
    }

    if (isMismatchError(errorLog)) {
      resolveError(errorLog);
    }

    return firstSeries.getInt("id");
  }

  private void attachPossibleSeries(Series series, JSONArray seriesNodes) throws SQLException {
    int possibleSeries = Math.min(5, seriesNodes.length());
    for (int i = 0; i < possibleSeries; i++) {
      JSONObject seriesNode = seriesNodes.getJSONObject(i);

      String tvdbSeriesName = seriesNode.getString("seriesName");
      Integer tvdbSeriesId = seriesNode.getInt("id");

      series.addPossibleSeriesMatch(connection, tvdbSeriesId, tvdbSeriesName);
    }
  }

  private String getTitleToCheck(String seriesTitle, @Nullable ErrorLog errorLog) {
    if (errorLog != null && isNotFoundError(errorLog)) {
      String chosenName = errorLog.chosenName.getValue();
      if (chosenName == null) {
        return seriesTitle;
      }
      return chosenName;
    }
    return seriesTitle;
  }

  private boolean isNotFoundError(@Nullable ErrorLog errorLog) {
    return errorLog != null && "NoMatchFound".equals(errorLog.errorType.getValue());
  }

  private boolean isMismatchError(@Nullable ErrorLog errorLog) {
    return errorLog != null && "NameMismatch".equals(errorLog.errorType.getValue());
  }

  private boolean shouldIgnoreShow(@Nullable ErrorLog errorLog) {
    return errorLog != null && Boolean.TRUE.equals(errorLog.ignoreError.getValue());
  }

  private void updateSeriesTitle(Series series, @NotNull ErrorLog errorLog) throws SQLException {
    String chosenName = errorLog.chosenName.getValue();
    String seriesTitle = series.seriesTitle.getValue();

    if (!seriesTitle.equalsIgnoreCase(chosenName)) {
      series.seriesTitle.changeValue(chosenName);
      series.commit(connection);
    }
  }

  private void markSeriesToIgnore(Series series) throws SQLException {
    series.ignoreTVDB.changeValue(true);
    series.commit(connection);
  }

  private boolean shouldAcceptMismatch(@Nullable ErrorLog errorLog) {
    if (errorLog == null) {
      return false;
    }
    if (!isMismatchError(errorLog)) {
      return false;
    }

    String chosenName = errorLog.chosenName.getValue();

    return chosenName != null && !"".equals(chosenName);
  }

  private void resolveError(ErrorLog errorLog) throws SQLException {
    errorLog.resolved.changeValue(true);
    errorLog.resolvedDate.changeValue(new Date());
    errorLog.commit(connection);
  }

  private void updateShowData(Series series) throws SQLException,  UnirestException {
    Integer tvdbID = series.tvdbSeriesExtId.getValue();
    String seriesTitle = series.seriesTitle.getValue();

    JSONObject seriesRoot = tvdbDataProvider.getSeriesData(tvdbID);

    debug(seriesTitle + ": Data found, updating.");

    JSONObject seriesJson = seriesRoot.getJSONObject("data");

    ResultSet existingTVDBSeries = findExistingTVDBSeries(tvdbID);

    TVDBSeries tvdbSeries = new TVDBSeries();
    if (existingTVDBSeries.next()) {
      tvdbSeries.initializeFromDBObject(existingTVDBSeries);
    } else {
      tvdbSeries.initializeForInsert();
    }

    String tvdbSeriesName = jsonReader.getStringWithKey(seriesJson, "seriesName");

    Integer id = jsonReader.getIntegerWithKey(seriesJson, "id");

    tvdbSeries.tvdbSeriesExtId.changeValue(id);
    tvdbSeries.name.changeValue(tvdbSeriesName);
    tvdbSeries.airsDayOfWeek.changeValue(jsonReader.getNullableStringWithKey(seriesJson, "airsDayOfWeek"));
    tvdbSeries.airsTime.changeValue(jsonReader.getNullableStringWithKey(seriesJson, "airsTime"));
    tvdbSeries.firstAired.changeValueFromString(jsonReader.getNullableStringWithKey(seriesJson, "firstAired"));
    tvdbSeries.network.changeValue(jsonReader.getNullableStringWithKey(seriesJson, "network"));
    tvdbSeries.overview.changeValue(jsonReader.getNullableStringWithKey(seriesJson, "overview"));
    tvdbSeries.rating.changeValue(jsonReader.getNullableDoubleWithKey(seriesJson, "siteRating"));
    tvdbSeries.ratingCount.changeValue(jsonReader.getNullableIntegerWithKey(seriesJson, "siteRatingCount"));
    tvdbSeries.runtime.changeValueFromString(jsonReader.getNullableStringWithKey(seriesJson, "runtime"));
    tvdbSeries.status.changeValue(jsonReader.getNullableStringWithKey(seriesJson, "status"));

    tvdbSeries.banner.changeValueFromString(jsonReader.getNullableStringWithKey(seriesJson, "banner"));

    // todo: change to integer in data model
    tvdbSeries.lastUpdated.changeValueFromString(((Integer)seriesJson.getInt("lastUpdated")).toString());
    tvdbSeries.imdbId.changeValueFromString(jsonReader.getNullableStringWithKey(seriesJson, "imdbId"));
    tvdbSeries.zap2it_id.changeValueFromString(jsonReader.getNullableStringWithKey(seriesJson, "zap2itId"));

    // todo: 'added' field
    // todo: 'networkid' field

    // todo: add api_version column to tvdb_series and tvdb_episode, and change it when this finishes processing.
    // todo: create api_change_log table and add a row for each change to series or episode
    // todo: create tvdb_error_log table and log any json format issues where non-nullable are null, or values are wrong type.


    // todo: create posters array
    JSONObject imageData = tvdbDataProvider.getPosterData(tvdbID);
    @NotNull JSONArray images = jsonReader.getArrayWithKey(imageData, "data");

    JSONObject firstImage = images.getJSONObject(images.length()-1);
    @NotNull String imageName = jsonReader.getStringWithKey(firstImage, "fileName");
    tvdbSeries.poster.changeValue(imageName);

    if (tvdbSeries.hasChanged()) {
      addChangeLogs(tvdbSeries);
    }

    tvdbSeries.commit(connection);

    series.tvdbSeriesId.changeValue(tvdbSeries.id.getValue());
    series.lastTVDBUpdate.changeValue(new Date());

    series.commit(connection);

    Integer seriesEpisodesAdded = 0;
    Integer seriesEpisodesUpdated = 0;

    Integer pageNumber = 1;
    Integer lastPage;

    do {
      JSONObject episodeData = tvdbDataProvider.getEpisodeSummaries(tvdbID, pageNumber);

      JSONObject links = episodeData.getJSONObject("links");
      lastPage = jsonReader.getIntegerWithKey(links, "last");

      JSONArray episodeArray = episodeData.getJSONArray("data");

      for (int i = 0; i < episodeArray.length(); i++) {
        JSONObject episode = episodeArray.getJSONObject(i);

        Integer episodeRemoteId = episode.getInt("id");

        try {
          TVDBEpisodeV2Updater tvdbEpisodeUpdater = new TVDBEpisodeV2Updater(series, connection, tvdbDataProvider, episodeRemoteId, new JSONReaderImpl(), false);
          TVDBEpisodeV2Updater.EPISODE_RESULT episodeResult = tvdbEpisodeUpdater.updateSingleEpisode();

          if (episodeResult == TVDBEpisodeV2Updater.EPISODE_RESULT.ADDED) {
            seriesEpisodesAdded++;
          } else if (episodeResult == TVDBEpisodeV2Updater.EPISODE_RESULT.UPDATED) {
            seriesEpisodesUpdated++;
          }
        } catch (Exception e) {
          debug("TVDB update of episode failed: ");
          e.printStackTrace();
          episodesFailed++;
          updateEpisodeLastError(episodeRemoteId);
          addMigrationError(series, episodeRemoteId, e);
        }
      }

      pageNumber++;

    } while (pageNumber <= lastPage);

    series.tvdbNew.changeValue(false);
    series.commit(connection);

    // Change API version if no episodes failed.
    if (episodesFailed == 0) {
      tvdbSeries.apiVersion.changeValue(2);
      tvdbSeries.commit(connection);
    }

    debug(seriesTitle + ": Update complete! Added: " + seriesEpisodesAdded + "; Updated: " + seriesEpisodesUpdated);

  }

  private void updateEpisodeLastError(Integer tvdbEpisodeExtId) {
    String sql = "SELECT e.* " +
        "FROM episode e " +
        "INNER JOIN tvdb_episode te " +
        " ON te.id = e.tvdb_episode_id " +
        "WHERE te.tvdb_episode_ext_id = ? " +
        "AND te.retired = ?";
    try {
      @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, tvdbEpisodeExtId, 0);
      if (resultSet.next()) {
        Episode episode = new Episode();
        episode.initializeFromDBObject(resultSet);
        episode.lastTVDBError.changeValue(new Date());
        episode.commit(connection);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
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

  private void addChangeLogs(TVDBSeries tvdbSeries) throws SQLException {
    for (FieldValue fieldValue : tvdbSeries.getChangedFields()) {
      TVDBMigrationLog tvdbMigrationLog = new TVDBMigrationLog();
      tvdbMigrationLog.initializeForInsert();

      tvdbMigrationLog.tvdbSeriesId.changeValue(tvdbSeries.id.getValue());

      tvdbMigrationLog.tvdbFieldName.changeValue(fieldValue.getFieldName());
      tvdbMigrationLog.oldValue.changeValue(fieldValue.getOriginalValue() == null ?
          null :
          fieldValue.getOriginalValue().toString());
      tvdbMigrationLog.newValue.changeValue(fieldValue.getChangedValue() == null ?
          null :
          fieldValue.getChangedValue().toString());

      tvdbMigrationLog.commit(connection);
    }
  }


  private ResultSet findExistingTVDBSeries(Integer tvdbRemoteId) throws SQLException {
    return connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM tvdb_series " +
            "WHERE tvdb_series_ext_id = ?",
        tvdbRemoteId
    );
  }


  private void addShowNotFoundErrorLog(Series series, String formattedName, String context) throws SQLException {
    ErrorLog errorLog = new ErrorLog();
    errorLog.initializeForInsert();

    errorLog.tivoName.changeValue(series.tivoName.getValue());
    errorLog.formattedName.changeValue(formattedName);
    errorLog.context.changeValue(context);
    errorLog.errorType.changeValue("NoMatchFound");
    errorLog.errorMessage.changeValue("Unable to find TVDB show with TiVo Name.");

    addBasicErrorLog(series.tivoSeriesExtId.getValue(), errorLog);
  }


  private void addMismatchErrorLog(String tivoId, String tivoName, String formattedName, String tvdbName) throws SQLException {
    ErrorLog errorLog = new ErrorLog();
    errorLog.initializeForInsert();

    errorLog.tivoName.changeValue(tivoName);
    errorLog.formattedName.changeValue(formattedName);
    errorLog.tvdbName.changeValue(tvdbName);
    errorLog.errorType.changeValue("NameMismatch");
    errorLog.errorMessage.changeValue("Mismatch between TiVo and TVDB names.");

    addBasicErrorLog(tivoId, errorLog);
  }

  private void addBasicErrorLog(String tivoId, ErrorLog errorLog) throws SQLException {
    errorLog.tivoId.changeValue(tivoId);
    errorLog.eventDate.changeValue(new Date());
    errorLog.resolved.changeValue(false);
    errorLog.commit(connection);
  }

  private void addErrorLog(String tivoId, String errorMessage) throws SQLException {
    ErrorLog errorLog = new ErrorLog();
    errorLog.initializeForInsert();

    errorLog.tivoId.changeValue(tivoId);
    errorLog.eventDate.changeValue(new Date());
    errorLog.errorMessage.changeValue(errorMessage);
    errorLog.resolved.changeValue(false);
    errorLog.commit(connection);
  }

  protected void debug(Object object) {
    System.out.println(object);
  }


  Integer getEpisodesAdded() {
    return _episodesAdded;
  }

  Integer getEpisodesUpdated() {
    return _episodesUpdated;
  }

}
