package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.mediaobject.*;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.NodeReader;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sun.istack.internal.NotNull;
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

  SeriesPostgres _series;

  private SQLConnection connection;
  private NodeReader nodeReader;

  Integer _episodesAdded = 0;
  Integer _episodesUpdated = 0;

  public TVDBSeriesPostgresUpdater(SQLConnection connection,
                                   @NotNull SeriesPostgres series,
                                   @NotNull NodeReader nodeReader) {
    this._series = series;
    this.connection = connection;
    this.nodeReader = nodeReader;
  }


  public void updateSeries() throws SQLException, BadlyFormattedXMLException {
    String seriesTitle = _series.seriesTitle.getValue();
    String seriesTiVoId = _series.tivoSeriesId.getValue();

    DBObject errorLog = getErrorLog(seriesTiVoId);

    if (shouldIgnoreShow(errorLog)) {
      markSeriesToIgnore(_series);
      resolveError(errorLog);
    } else {


      Boolean matchedWrong = Boolean.TRUE.equals(_series.matchedWrong.getValue());
      Integer existingId = _series.tvdbId.getValue();

      Integer tvdbId = (existingId == null || matchedWrong) ?
          getTVDBID(_series, errorLog) :
          existingId;

      Boolean usingOldWrongID = matchedWrong && Objects.equals(existingId, tvdbId);

      if (tvdbId != null && !usingOldWrongID) {
        debug(seriesTitle + ": ID found, getting show data.");
        _series.tvdbId.changeValue(tvdbId);

        if (matchedWrong) {
          Integer seriesId = _series.id.getValue();
          removeTVDBOnlyEpisodes(seriesId);
          clearTVDBIds(seriesId);
          _series.needsTVDBRedo.changeValue(false);
          _series.matchedWrong.changeValue(false);
        }

        updateShowData(_series);

      }
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

  private DBObject getErrorLog(String tivoId) {
    /*
    BasicDBObject query = new BasicDBObject("TiVoID", tivoId)
        .append("Resolved", false);
    return _db.getCollection("errorlogs").findOne(query);
    */
    // todo: update
    return null;
  }

  private Integer getTVDBID(SeriesPostgres series, DBObject errorLog) throws SQLException, BadlyFormattedXMLException {
    String seriesTitle = series.seriesTitle.getValue();
    String tivoId = series.tivoSeriesId.getValue();
    String tvdbHint = series.tvdbHint.getValue();

    String titleToCheck = tvdbHint == null ?
        getTitleToCheck(seriesTitle, errorLog) :
        tvdbHint;

    if (titleToCheck == null) {
      throw new RuntimeException("Title to check is null. TiVoSeriesId: " + tivoId);
    }

    String formattedTitle = titleToCheck
        .toLowerCase()
        .replaceAll(" ", "_");

    debug("Update for: " + seriesTitle + ", formatted as '" + formattedTitle + "'");

    String tvdbUrl = "http://thetvdb.com/api/GetSeries.php?seriesname=" + formattedTitle;

    Document document;
    try {
      document = nodeReader.readXMLFromUrl(tvdbUrl);
    } catch (SAXException | IOException e) {
      e.printStackTrace();
      addShowNotFoundErrorLog(series, formattedTitle, "HTTP Timeout");
      return null;
    }

    NodeList nodeList = document.getChildNodes();

    NodeList dataNode = nodeReader.getNodeWithTag(nodeList, "Data").getChildNodes();

    List<Node> seriesNodes = nodeReader.getAllNodesWithTag(dataNode, "Series");

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
        updateSeriesTitle(tivoId, seriesTitle, errorLog);
      } else {
        debug("Discrepency between TiVo and TVDB names!");
        attachPossibleSeries(series, seriesNodes);

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

  private void attachPossibleSeries(SeriesPostgres series, List<Node> seriesNodes) throws SQLException {
    int possibleSeries = Math.min(5, seriesNodes.size());
    for (int i = 0; i < possibleSeries; i++) {
      NodeList seriesNode = seriesNodes.get(i).getChildNodes();

      String tvdbSeriesName = nodeReader.getValueOfSimpleStringNode(seriesNode, "SeriesName");
      Integer tvdbSeriesId = Integer.parseInt(nodeReader.getValueOfSimpleStringNode(seriesNode, "id"));

      connection.prepareAndExecuteStatementUpdate(
          "INSERT INTO possible_series_match (series_id, tvdb_series_title, tvdb_series_id) " +
              "VALUES (?, ?, ?)",
          series.id.getValue(),
          tvdbSeriesName,
          tvdbSeriesId
      );
    }
  }

  private String getTitleToCheck(String seriesTitle, DBObject errorLog) {
    if (errorLog != null && isNotFoundError(errorLog)) {
      Object chosenName = errorLog.get("ChosenName");
      if (chosenName == null) {
        return seriesTitle;
      }
      return (String) chosenName;
    }
    return seriesTitle;
  }

  private boolean isNotFoundError(DBObject errorLog) {
    return errorLog != null && "NoMatchFound".equals(errorLog.get("ErrorType"));
  }

  private boolean isMismatchError(DBObject errorLog) {
    return errorLog != null && "NameMismatch".equals(errorLog.get("ErrorType"));
  }

  private boolean shouldIgnoreShow(DBObject errorLog) {
    return errorLog != null && Boolean.TRUE.equals(errorLog.get("IgnoreError"));
  }

  private void updateSeriesTitle(String tivoId, String seriesTitle, DBObject errorLog) throws SQLException {
    String chosenName = (String) errorLog.get("ChosenName");

    if (!seriesTitle.equalsIgnoreCase(chosenName)) {
      connection.prepareAndExecuteStatementUpdate(
          "UPDATE series " +
              "SET title = ? " +
              "WHERE tivo_series_id = ?",
          chosenName,
          tivoId
      );
    }
  }

  private void markSeriesToIgnore(SeriesPostgres series) throws SQLException {
    series.ignoreTVDB.changeValue(true);
    series.commit(connection);
  }

  private boolean shouldAcceptMismatch(DBObject errorLog) {
    if (!isMismatchError(errorLog)) {
      return false;
    }

    String chosenName = (String) errorLog.get("ChosenName");

    return chosenName != null && !"".equals(chosenName);
  }

  private void resolveError(DBObject errorLog) {
    /*
    BasicDBObject queryObject = new BasicDBObject("_id", errorLog.get("_id"));

    BasicDBObject updateObject = new BasicDBObject()
        .append("Resolved", true)
        .append("ResolvedDate", new Date());

    updateCollectionWithQuery("errorlogs", queryObject, updateObject);
    */

    // todo: do this
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

      tvdbEpisode.tvdbId.changeValue(tvdbRemoteId);
      tvdbEpisode.absoluteNumber.changeValueFromString(nodeReader.getValueOfSimpleStringNode(episodeNode, "absoute_number"));
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
      episode.episodeNumber.changeValueFromString(episodenumber);
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



  private void addShowNotFoundErrorLog(SeriesPostgres series, String formattedName, String context) {
    /*

    BasicDBObject object = new BasicDBObject()
        .append("TiVoName", series.tivoName.getValue())
        .append("FormattedName", formattedName)
        .append("Context", context)
        .append("ErrorType", "NoMatchFound")
        .append("ErrorMessage", "Unable to find TVDB show with TiVo Name.");

    addBasicErrorLog(series.tivoSeriesId.getValue(), object);*/
  }


  private void addMismatchErrorLog(String tivoId, String tivoName, String formattedName, String tvdbName) {
    BasicDBObject object = new BasicDBObject()
        .append("TiVoName", tivoName)
        .append("FormattedName", formattedName)
        .append("TVDBName", tvdbName)
        .append("ErrorType", "NameMismatch")
        .append("ErrorMessage", "Mismatch between TiVo and TVDB names.");

    addBasicErrorLog(tivoId, object);
  }

  private void addBasicErrorLog(String tivoId, BasicDBObject errorObject) {
    /*
    DBCollection errorlogs = _db.getCollection("errorlogs");
    errorObject
        .append("TiVoID", tivoId)
        .append("EventDate", new Date())
        .append("Resolved", false)
        .append("ResolvedDate", null);

    try {
      errorlogs.insert(errorObject);
    } catch (MongoException e) {
      throw new RuntimeException("Error inserting error log into database.\r\n" + e.getLocalizedMessage());
    }
    */
  }

  private void addErrorLog(String tivoId, String errorMessage) {
    /*
    DBCollection errorlogs = _db.getCollection("errorlogs");
    BasicDBObject errorLog = new BasicDBObject()
        .append("TiVoID", tivoId)
        .append("EventDate", new Date())
        .append("ErrorMessage", errorMessage)
        .append("Resolved", false)
        .append("ResolvedDate", null);

    try {
      errorlogs.insert(errorLog);
    } catch (MongoException e) {
      throw new RuntimeException("Error inserting error log into database.\r\n" + e.getLocalizedMessage());
    }
    */
  }

  protected void debug(Object object) {
    System.out.println(object);
  }



}
