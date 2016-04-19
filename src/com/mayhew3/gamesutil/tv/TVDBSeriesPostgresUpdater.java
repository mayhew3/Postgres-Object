package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.dataobject.*;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.NodeReader;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
    Integer seriesId = series.id.getValue();

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
      Integer tvdbRemoteId = Integer.valueOf(nodeReader.getValueOfSimpleStringNode(episodeNode, "id"));

      String episodenumber = nodeReader.getValueOfSimpleStringNode(episodeNode, "episodenumber");
      String episodename = nodeReader.getValueOfSimpleStringNode(episodeNode, "episodename");
      String seasonnumber = nodeReader.getValueOfSimpleStringNode(episodeNode, "seasonnumber");
      String firstaired = nodeReader.getValueOfSimpleStringNode(episodeNode, "firstaired");

      ResultSet existingTVDBRow = findExistingTVDBEpisode(tvdbRemoteId);
      Boolean matched = false;
      Boolean added = false;

      TVDBEpisodePostgres tvdbEpisode = new TVDBEpisodePostgres();
      EpisodePostgres episode = new EpisodePostgres();

      if (!existingTVDBRow.next()) {
        tvdbEpisode.initializeForInsert();

        // todo: Optimization: skip looking for match when firstAired is future. Obviously it's not on the TiVo yet.
        TiVoEpisodePostgres tivoEpisode = findTiVoMatch(episodename, seasonnumber, episodenumber, firstaired, seriesId);

        if (tivoEpisode == null) {
          episode.initializeForInsert();
          added = true;
        } else {

          // todo: handle multiple rows returned
          ResultSet episodeRow = getEpisodeFromTiVoEpisodeID(tivoEpisode.id.getValue());
          episode.initializeFromDBObject(episodeRow);
          matched = true;
        }

      } else {
        tvdbEpisode.initializeFromDBObject(existingTVDBRow);
        ResultSet episodeRow = getEpisodeFromTVDBEpisodeID(tvdbEpisode.id.getValue());
        episode.initializeFromDBObject(episodeRow);
      }

      // todo: Add log entry for when TVDB values change.

      String absoluteNumber = nodeReader.getValueOfSimpleStringNode(episodeNode, "absoute_number");

      tvdbEpisode.tvdbId.changeValue(tvdbRemoteId);
      tvdbEpisode.absoluteNumber.changeValueFromString(absoluteNumber);
      tvdbEpisode.seasonNumber.changeValueFromString(seasonnumber);
      tvdbEpisode.episodeNumber.changeValueFromString(episodenumber);
      tvdbEpisode.name.changeValueFromString(episodename);
      tvdbEpisode.firstAired.changeValueFromString(firstaired);
      tvdbEpisode.overview.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "overview"));
      tvdbEpisode.productionCode.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "ProductionCode"));
      tvdbEpisode.rating.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "Rating"));
      tvdbEpisode.ratingCount.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "RatingCount"));
      tvdbEpisode.director.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "Director"));
      tvdbEpisode.writer.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "Writer"));
      tvdbEpisode.lastUpdated.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "lastupdated"));
      tvdbEpisode.seasonId.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "seasonid"));
      tvdbEpisode.filename.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "filename"));
      tvdbEpisode.airsAfterSeason.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "airsafter_season"));
      tvdbEpisode.airsBeforeSeason.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "airsbefore_season"));
      tvdbEpisode.airsBeforeEpisode.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "airsbefore_episode"));
      tvdbEpisode.thumbHeight.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "thumb_height"));
      tvdbEpisode.thumbWidth.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "thumb_width"));


      tvdbEpisode.commit(connection);

      episode.seriesId.changeValue(seriesId);
      episode.seriesTitle.changeValueFromString(seriesTitle);
      episode.tvdbEpisodeId.changeValue(tvdbEpisode.id.getValue());
      episode.title.changeValue(episodename);
      episode.season.changeValueFromString(seasonnumber);
      episode.episodeNumber.changeValueFromString(absoluteNumber);
      episode.seasonEpisodeNumber.changeValueFromString(episodenumber);
      episode.airDate.changeValueFromString(firstaired);

      // todo: add or get season object


      episode.commit(connection);

      if (added) {
        _episodesAdded++;
        seriesEpisodesAdded++;
      } else {
        _episodesUpdated++;
        seriesEpisodesUpdated++;
      }

      Integer episodeId = tvdbEpisode.id.getValue();

      if (episodeId == null) {
        throw new RuntimeException("_id wasn't populated on Episode with tvdbEpisodeId " + tvdbRemoteId + " after insert.");
      } else {
        // add manual reference to episode to episodes array.

        updateSeriesDenorms(added, matched, series);

        series.commit(connection);
      }
    }

//    series = findSingleMatch(_db.getCollection("series"), "_id", seriesId);
//    verifyEpisodesArray(series);

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

  private ResultSet findExistingTVDBEpisode(Integer tvdbRemoteId) throws SQLException {
    return connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM tvdb_episode " +
            "WHERE tvdb_id = ?",
        tvdbRemoteId
    );
  }

  private ResultSet getEpisodeFromTiVoEpisodeID(Integer tivoEpisodeID) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT e.* " +
            "FROM episode e " +
            "INNER edge_tivo_episode ete " +
            "  ON ete.episode_id = e.id " +
            "WHERE ete.tivo_episode_id = ?",
        tivoEpisodeID
    );
    if (!resultSet.next()) {
      throw new RuntimeException("No row in episode matching tivo_episode_id " + tivoEpisodeID);
    }

    return resultSet;
  }

  private ResultSet getEpisodeFromTVDBEpisodeID(Integer tvdbEpisodeID) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM episode " +
            "WHERE tvdb_episode_id = ?",
        tvdbEpisodeID
    );
    if (!resultSet.next()) {
      throw new RuntimeException("No row in episode matching tvdb_episode_id " + tvdbEpisodeID);
    }

    return resultSet;
  }


  private void updateSeriesDenorms(Boolean added, Boolean matched, SeriesPostgres series) {
    if (added) {
      series.tvdbOnlyEpisodes.increment(1);
      series.unwatchedUnrecorded.increment(1);
    }
    if (matched) {
      series.matchedEpisodes.increment(1);
      series.unmatchedEpisodes.increment(-1);
    }
  }



  // todo: Handle finding two TiVo matches.
  private TiVoEpisodePostgres findTiVoMatch(String episodeTitle, String tvdbSeasonStr, String tvdbEpisodeNumberStr, String firstAiredStr, Integer seriesId) throws SQLException {
    List<TiVoEpisodePostgres> matchingEpisodes = new ArrayList<>();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT te.* " +
            "FROM tivo_episode te " +
            "INNER JOIN edge_tivo_episode ete " +
            "  ON ete.tivo_episode_id = te.id " +
            "INNER JOIN episode e " +
            "  ON ete.episode_id = e.id " +
            "WHERE e.seriesid = ? " +
            "AND e.tvdb_episode_id IS NULL " +
            "AND e.retired = ?",
        seriesId,
        0
    );

    List<TiVoEpisodePostgres> episodes = new ArrayList<>();

    while(resultSet.next()) {
      TiVoEpisodePostgres episode = new TiVoEpisodePostgres();
      episode.initializeFromDBObject(resultSet);
      episodes.add(episode);
    }

    if (episodeTitle != null) {
      for (TiVoEpisodePostgres episode : episodes) {
        String tivoTitleObject = episode.title.getValue();
        if (episodeTitle.equalsIgnoreCase(tivoTitleObject)) {
          matchingEpisodes.add(episode);
        }
      }
    }

    if (matchingEpisodes.size() == 1) {
      return matchingEpisodes.get(0);
    } else if (matchingEpisodes.size() > 1) {
      debug("Found " + matchingEpisodes.size() + " matching episodes for " +
          tvdbSeasonStr + "x" + tvdbEpisodeNumberStr + " " +
          "'" + episodeTitle + "'.");
      return null;
    }

    // no match found on episode title. Try episode number.


    if (tvdbEpisodeNumberStr != null && tvdbSeasonStr != null) {
      Integer tvdbSeason = Integer.valueOf(tvdbSeasonStr);
      Integer tvdbEpisodeNumber = Integer.valueOf(tvdbEpisodeNumberStr);

      for (TiVoEpisodePostgres episode : episodes) {

        Integer tivoEpisodeNumber = episode.episodeNumber.getValue();

        if (tivoEpisodeNumber != null) {

          Integer tivoSeasonNumber = 1;

          if (tivoEpisodeNumber < 100) {
            if (Objects.equals(tivoSeasonNumber, tvdbSeason) && Objects.equals(tivoEpisodeNumber, tvdbEpisodeNumber)) {
              matchingEpisodes.add(episode);
            }
          } else {
            String tiVoEpisodeNumberStr = tivoEpisodeNumber.toString();
            int seasonLength = tiVoEpisodeNumberStr.length() / 2;

            String tivoSeasonStr = tiVoEpisodeNumberStr.substring(0, seasonLength);
            String tivoEpisodeNumberStr = tiVoEpisodeNumberStr.substring(seasonLength, tiVoEpisodeNumberStr.length());

            tivoEpisodeNumber = Integer.valueOf(tivoEpisodeNumberStr);
            tivoSeasonNumber = Integer.valueOf(tivoSeasonStr);

            if (Objects.equals(tivoSeasonNumber, tvdbSeason) && Objects.equals(tivoEpisodeNumber, tvdbEpisodeNumber)) {
              matchingEpisodes.add(episode);
            }
          }
        }
      }

    }


    if (matchingEpisodes.size() == 1) {
      return matchingEpisodes.get(0);
    } else if (matchingEpisodes.size() > 1) {
      debug("Found " + matchingEpisodes.size() + " matching episodes for " +
          tvdbSeasonStr + "x" + tvdbEpisodeNumberStr + " " +
          "'" + episodeTitle + "'.");
      return null;
    }


    // no match on episode number. Try air date.

    if (firstAiredStr != null) {
      DateTime firstAired = new DateTime(firstAiredStr);

      for (TiVoEpisodePostgres episode : episodes) {
        Date showingStartTimeObj = episode.showingStartTime.getValue();

        if (showingStartTimeObj != null) {
          DateTime showingStartTime = new DateTime(showingStartTimeObj);

          DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();

          if (comparator.compare(firstAired, showingStartTime) == 0) {
            matchingEpisodes.add(episode);
          }
        }
      }

    }


    if (matchingEpisodes.size() == 1) {
      return matchingEpisodes.get(0);
    } else if (matchingEpisodes.size() > 1) {
      debug("Found " + matchingEpisodes.size() + " matching episodes for " +
          tvdbSeasonStr + "x" + tvdbEpisodeNumberStr + " " +
          "'" + episodeTitle + "'.");
      return null;
    } else {
      debug("Found no matches for " +
          tvdbSeasonStr + "x" + tvdbEpisodeNumberStr + " " +
          "'" + episodeTitle + "'.");
      return null;
    }

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
