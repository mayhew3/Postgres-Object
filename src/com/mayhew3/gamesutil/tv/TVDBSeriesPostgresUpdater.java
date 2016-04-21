package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.dataobject.*;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.NodeReader;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TVDBSeriesPostgresUpdater {

  SeriesPostgres series;

  private SQLConnection connection;
  private NodeReader nodeReader;

  Integer _episodesAdded = 0;
  Integer _episodesUpdated = 0;

  public TVDBSeriesPostgresUpdater(SQLConnection connection,
                                   @NotNull SeriesPostgres series,
                                   @NotNull NodeReader nodeReader) {
    this.series = series;
    this.connection = connection;
    this.nodeReader = nodeReader;
  }


  public void updateSeries() throws SQLException, BadlyFormattedXMLException, ShowFailedException {
    String seriesTitle = series.seriesTitle.getValue();
    String seriesTiVoId = series.tivoSeriesId.getValue();

    ErrorLogPostgres errorLog = getErrorLog(seriesTiVoId);

    if (shouldIgnoreShow(errorLog)) {
      markSeriesToIgnore(series);
      resolveError(errorLog);
    } else {

      Boolean matchedWrong = Boolean.TRUE.equals(series.matchedWrong.getValue());
      Integer existingId = series.tvdbId.getValue();

      Integer tvdbId = getTVDBID(errorLog, matchedWrong, existingId);

      Boolean usingOldWrongID = matchedWrong && Objects.equals(existingId, tvdbId);

      if (tvdbId != null && !usingOldWrongID) {
        debug(seriesTitle + ": ID found, getting show data.");
        series.tvdbId.changeValue(tvdbId);

        if (matchedWrong) {
          Integer seriesId = series.id.getValue();
          removeTVDBOnlyEpisodes(seriesId);
          clearTVDBIds(seriesId);
          series.needsTVDBRedo.changeValue(false);
          series.matchedWrong.changeValue(false);
        }

        updateShowData(series);

      }
    }
  }

  // todo: never return null, just throw exception if failed to find
  private Integer getTVDBID(ErrorLogPostgres errorLog, Boolean matchedWrong, Integer existingId) throws SQLException, ShowFailedException, BadlyFormattedXMLException {
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

  private void removeTVDBOnlyEpisodes(Integer seriesId) throws SQLException {

    connection.prepareAndExecuteStatementUpdate(
        "UPDATE tvdb_episode " +
            "SET retired = id " +
            "WHERE id IN (SELECT tvdb_episode_id " +
            "           FROM episode " +
            "           WHERE series_id = ?)",
        seriesId
    );

    connection.prepareAndExecuteStatementUpdate(
        "UPDATE episode " +
            "SET retired = id " +
            "WHERE series_id = ? " +
            "AND on_tivo <> ?",
        seriesId,
        true
    );

  }

  private void clearTVDBIds(Integer seriesId) throws SQLException {

    connection.prepareAndExecuteStatementUpdate(
        "UPDATE episode " +
            "SET tvdb_episode_id = NULL " +
            "WHERE series_id = ? " +
            "AND on_tivo = ?",
        seriesId,
        true
    );

  }

  @Nullable
  private ErrorLogPostgres getErrorLog(String tivoId) throws SQLException {
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
      ErrorLogPostgres errorLog = new ErrorLogPostgres();
      errorLog.initializeFromDBObject(resultSet);
      return errorLog;
    }

    return null;
  }

  private Integer findTVDBMatch(SeriesPostgres series, ErrorLogPostgres errorLog) throws SQLException, BadlyFormattedXMLException, IOException, SAXException {
    String seriesTitle = series.seriesTitle.getValue();
    String tivoId = series.tivoSeriesId.getValue();
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

    List<Node> seriesNodes = getSeriesNodes(formattedTitle);

    if (seriesNodes.isEmpty()) {
      debug("Show not found!");
      if (!isNotFoundError(errorLog)) {
        addShowNotFoundErrorLog(series, formattedTitle, "Empty result found.");
      }
      return null;
    }

    if (isNotFoundError(errorLog)) {
      resolveError(errorLog);
    }

    NodeList firstSeries = seriesNodes.get(0).getChildNodes();
    String seriesName = nodeReader.getValueOfSimpleStringNode(firstSeries, "SeriesName");

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

    String id = nodeReader.getValueOfSimpleStringNode(firstSeries, "id");
    return id == null ? null : Integer.parseInt(id);
  }

  private List<Node> getSeriesNodes(String formattedTitle) throws IOException, SAXException, BadlyFormattedXMLException {
    String tvdbUrl = "http://thetvdb.com/api/GetSeries.php?seriesname=" + formattedTitle;
    Document document = nodeReader.readXMLFromUrl(tvdbUrl);
    NodeList nodeList = document.getChildNodes();
    NodeList dataNode = nodeReader.getNodeWithTag(nodeList, "Data").getChildNodes();

    return nodeReader.getAllNodesWithTag(dataNode, "Series");
  }

  private void attachPossibleSeries(SeriesPostgres series, List<Node> seriesNodes) throws SQLException {
    int possibleSeries = Math.min(5, seriesNodes.size());
    for (int i = 0; i < possibleSeries; i++) {
      NodeList seriesNode = seriesNodes.get(i).getChildNodes();

      String tvdbSeriesName = nodeReader.getValueOfSimpleStringNode(seriesNode, "SeriesName");
      Integer tvdbSeriesId = Integer.parseInt(nodeReader.getValueOfSimpleStringNode(seriesNode, "id"));

      series.addPossibleSeriesMatch(connection, tvdbSeriesId, tvdbSeriesName);
    }
  }

  private String getTitleToCheck(String seriesTitle, ErrorLogPostgres errorLog) {
    if (errorLog != null && isNotFoundError(errorLog)) {
      String chosenName = errorLog.chosenName.getValue();
      if (chosenName == null) {
        return seriesTitle;
      }
      return chosenName;
    }
    return seriesTitle;
  }

  private boolean isNotFoundError(@Nullable ErrorLogPostgres errorLog) {
    return errorLog != null && "NoMatchFound".equals(errorLog.errorType.getValue());
  }

  private boolean isMismatchError(@Nullable ErrorLogPostgres errorLog) {
    return errorLog != null && "NameMismatch".equals(errorLog.errorType.getValue());
  }

  private boolean shouldIgnoreShow(@Nullable ErrorLogPostgres errorLog) {
    return errorLog != null && Boolean.TRUE.equals(errorLog.ignoreError.getValue());
  }

  private void updateSeriesTitle(SeriesPostgres series, ErrorLogPostgres errorLog) throws SQLException {
    String chosenName = errorLog.chosenName.getValue();
    String seriesTitle = series.seriesTitle.getValue();

    if (!seriesTitle.equalsIgnoreCase(chosenName)) {
      series.seriesTitle.changeValue(chosenName);
      series.commit(connection);
    }
  }

  private void markSeriesToIgnore(SeriesPostgres series) throws SQLException {
    series.ignoreTVDB.changeValue(true);
    series.commit(connection);
  }

  private boolean shouldAcceptMismatch(ErrorLogPostgres errorLog) {
    if (!isMismatchError(errorLog)) {
      return false;
    }

    String chosenName = errorLog.chosenName.getValue();

    return chosenName != null && !"".equals(chosenName);
  }

  private void resolveError(ErrorLogPostgres errorLog) throws SQLException {
    errorLog.resolved.changeValue(true);
    errorLog.resolvedDate.changeValue(new Date());
    errorLog.commit(connection);
  }

  private void updateShowData(SeriesPostgres series) throws SQLException, BadlyFormattedXMLException {
    Integer tvdbID = series.tvdbId.getValue();
    String tivoSeriesId = series.tivoSeriesId.getValue();
    String seriesTitle = series.seriesTitle.getValue();

    String apiKey = "04DBA547465DC136";
    String url = "http://thetvdb.com/api/" + apiKey + "/series/" + tvdbID + "/all/en.xml";

    Document document;
    try {
      document = nodeReader.readXMLFromUrl(url);
    } catch (SAXException | IOException e) {
      e.printStackTrace();
      addErrorLog(tivoSeriesId, "Error calling API for TVDB ID " + tvdbID);
      return;
    }

    debug(seriesTitle + ": Data found, updating.");

    NodeList nodeList = document.getChildNodes();

    NodeList dataNode = nodeReader.getNodeWithTag(nodeList, "Data").getChildNodes();
    NodeList seriesNode = nodeReader.getNodeWithTag(dataNode, "Series").getChildNodes();

    ResultSet existingTVDBSeries = findExistingTVDBSeries(tvdbID);

    TVDBSeriesPostgres tvdbSeries = new TVDBSeriesPostgres();
    if (existingTVDBSeries.next()) {
      tvdbSeries.initializeFromDBObject(existingTVDBSeries);
    } else {
      tvdbSeries.initializeForInsert();
    }

    String tvdbSeriesName = nodeReader.getValueOfSimpleStringNode(seriesNode, "seriesname");

    tvdbSeries.tvdbId.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "id"));
    tvdbSeries.name.changeValueFromString(tvdbSeriesName);
    tvdbSeries.airsDayOfWeek.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "airs_dayofweek"));
    tvdbSeries.airsTime.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "airs_time"));
    tvdbSeries.firstAired.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "firstaired"));
    tvdbSeries.network.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "network"));
    tvdbSeries.overview.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "overview"));
    tvdbSeries.rating.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "rating"));
    tvdbSeries.ratingCount.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "ratingcount"));
    tvdbSeries.runtime.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "runtime"));
    tvdbSeries.tvdbSeriesId.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "SeriesID"));
    tvdbSeries.status.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "status"));
    tvdbSeries.poster.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "poster"));
    tvdbSeries.banner.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "banner"));
    tvdbSeries.lastUpdated.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "lastupdated"));
    tvdbSeries.imdbId.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "IMDB_ID"));
    tvdbSeries.zap2it_id.changeValueFromString(nodeReader.getValueOfSimpleStringNode(seriesNode, "zap2it_id"));

    tvdbSeries.commit(connection);

    series.tvdbSeriesId.changeValue(tvdbSeries.id.getValue());
    series.commit(connection);

    Integer seriesEpisodesAdded = 0;
    Integer seriesEpisodesUpdated = 0;

    List<Node> episodes = nodeReader.getAllNodesWithTag(dataNode, "Episode");


    for (Node episodeParent : episodes) {
      NodeList episodeNode = episodeParent.getChildNodes();

      try {
        TVDBEpisodePostgresUpdater tvdbEpisodePostgresUpdater = new TVDBEpisodePostgresUpdater(series, episodeNode, connection, nodeReader);
        TVDBEpisodePostgresUpdater.EPISODE_RESULT episodeResult = tvdbEpisodePostgresUpdater.updateSingleEpisode();

        if (episodeResult == TVDBEpisodePostgresUpdater.EPISODE_RESULT.ADDED) {
          seriesEpisodesAdded++;
        } else if (episodeResult == TVDBEpisodePostgresUpdater.EPISODE_RESULT.UPDATED) {
          seriesEpisodesUpdated++;
        }
      } catch (ShowFailedException e) {
        debug("TVDB update of episode failed: ");
        e.printStackTrace();
      } catch (SQLException e) {
        debug("SQL exception occured updating single episode for series " + seriesTitle);
        e.printStackTrace();
      }
    }

    debug(seriesTitle + ": Update complete! Added: " + seriesEpisodesAdded + "; Updated: " + seriesEpisodesUpdated);

  }

  private ResultSet findExistingTVDBSeries(Integer tvdbRemoteId) throws SQLException {
    return connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM tvdb_series " +
            "WHERE tvdb_id = ?",
        tvdbRemoteId
    );
  }


  private void addShowNotFoundErrorLog(SeriesPostgres series, String formattedName, String context) throws SQLException {
    ErrorLogPostgres errorLog = new ErrorLogPostgres();
    errorLog.initializeForInsert();

    errorLog.tivoName.changeValue(series.tivoName.getValue());
    errorLog.formattedName.changeValue(formattedName);
    errorLog.context.changeValue(context);
    errorLog.errorType.changeValue("NoMatchFound");
    errorLog.errorMessage.changeValue("Unable to find TVDB show with TiVo Name.");

    addBasicErrorLog(series.tivoSeriesId.getValue(), errorLog);
  }


  private void addMismatchErrorLog(String tivoId, String tivoName, String formattedName, String tvdbName) throws SQLException {
    ErrorLogPostgres errorLog = new ErrorLogPostgres();
    errorLog.initializeForInsert();

    errorLog.tivoName.changeValue(tivoName);
    errorLog.formattedName.changeValue(formattedName);
    errorLog.tvdbName.changeValue(tvdbName);
    errorLog.errorType.changeValue("NameMismatch");
    errorLog.errorMessage.changeValue("Mismatch between TiVo and TVDB names.");

    addBasicErrorLog(tivoId, errorLog);
  }

  private void addBasicErrorLog(String tivoId, ErrorLogPostgres errorLog) throws SQLException {
    errorLog.tivoId.changeValue(tivoId);
    errorLog.eventDate.changeValue(new Date());
    errorLog.resolved.changeValue(false);
    errorLog.commit(connection);
  }

  private void addErrorLog(String tivoId, String errorMessage) throws SQLException {
    ErrorLogPostgres errorLog = new ErrorLogPostgres();
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


  public Integer getEpisodesAdded() {
    return _episodesAdded;
  }

  public Integer getEpisodesUpdated() {
    return _episodesUpdated;
  }

}
