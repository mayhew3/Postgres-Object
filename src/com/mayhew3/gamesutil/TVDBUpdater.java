package com.mayhew3.gamesutil;

import com.mayhew3.gamesutil.mediaobject.Episode;
import com.mayhew3.gamesutil.mediaobject.Series;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

public class TVDBUpdater extends TVDatabaseUtility {

  private Integer seriesUpdates = 0;
  private Integer episodesAdded = 0;
  private Integer episodesUpdated = 0;

  public TVDBUpdater() throws UnknownHostException {
    super("tv");
  }

  public static void main(String[] args) {
    try {
      TVDBUpdater tvdbUpdater = new TVDBUpdater();
      tvdbUpdater.runUpdate();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }

  public void runUpdate() {

    try {
      updateShows();
      closeDatabase();
    } catch (RuntimeException e) {
      closeDatabase();
      throw e;
    }
  }

  private void updateShows() {
    BasicDBObject query = new BasicDBObject()
        .append("IsSuggestion", false)
        .append("IgnoreTVDB", new BasicDBObject("$ne", true))
        .append("SeriesId", new BasicDBObject("$exists", true))
        .append("SeriesTitle", "Better Call Saul")
        .append("IsEpisodic", true);

    DBCollection untaggedShows = _db.getCollection("series");
    DBCursor cursor = untaggedShows.find(query);

    int totalRows = cursor.count();
    debug(totalRows + " series found for update. Starting.");

    int i = 0;

    while (cursor.hasNext()) {
      i++;
      DBObject show = cursor.next();

      updateShow(show);

      debug(i + " out of " + totalRows + " processed.");
    }
  }

  private void updateShow(DBObject show) {
    Series series = new Series();
    series.initializeFromDBObject(show);

    String seriesTitle = series.seriesTitle.getValue();
    String tivoId = series.seriesId.getValue();

    DBObject errorLog = getErrorLog(tivoId);

    if (shouldIgnoreShow(errorLog)) {
      markSeriesToIgnore(series);
      resolveError(errorLog);
    } else {

      Integer existingId = series.tvdbId.getValue();

      Integer tvdbId = existingId == null ?
          getTVDBID(series, errorLog) :
          existingId;

      if (tvdbId != null) {
        debug(seriesTitle + ": ID found, getting show data.");
        series.tvdbId.changeValue(tvdbId);

        updateShowData(series);

      }
    }
  }

  private DBObject getErrorLog(String tivoId) {
    BasicDBObject query = new BasicDBObject("TiVoID", tivoId)
        .append("Resolved", false);
    return _db.getCollection("errorlogs").findOne(query);
  }

  private Integer getTVDBID(Series series, DBObject errorLog) {
    String seriesTitle = series.seriesTitle.getValue();
    String tivoId = series.seriesId.getValue();
    String tvdbHint = series.tvdbHint.getValue();

    String titleToCheck = tvdbHint == null ?
        getTitleToCheck(seriesTitle, errorLog) :
        tvdbHint;

    String formattedTitle = titleToCheck
        .toLowerCase()
        .replaceAll(" ", "_");

    debug("Update for: " + seriesTitle + ", formatted as '" + formattedTitle + "'");

    String tvdbUrl = "http://thetvdb.com/api/GetSeries.php?seriesname=" + formattedTitle;

    Document document;
    try {
      document = readXMLFromUrl(tvdbUrl);
    } catch (SAXException | IOException e) {
      e.printStackTrace();
      addShowNotFoundErrorLog(series, formattedTitle, "HTTP Timeout");
      return null;
    }

    NodeList nodeList = document.getChildNodes();

    NodeList dataNode = getNodeWithTag(nodeList, "Data").getChildNodes();

    List<Node> seriesNodes = getAllNodesWithTag(dataNode, "Series");

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
    String seriesName = getValueOfSimpleStringNode(firstSeries, "SeriesName");

    if (!seriesTitle.equalsIgnoreCase(seriesName) && !titleToCheck.equalsIgnoreCase(seriesName)) {
      if (shouldAcceptMismatch(errorLog)) {
        updateSeriesTitle(tivoId, seriesTitle, errorLog);
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

    return Integer.parseInt(getValueOfSimpleStringNode(firstSeries, "id"));
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

  private void updateSeriesTitle(String tivoId, String seriesTitle, DBObject errorLog) {
    String chosenName = (String) errorLog.get("ChosenName");
    if (!seriesTitle.equalsIgnoreCase(chosenName)) {
      BasicDBObject queryObject = new BasicDBObject("SeriesId", tivoId);
      BasicDBObject updateObject = new BasicDBObject("SeriesTitle", chosenName);

      updateCollectionWithQuery("series", queryObject, updateObject);
    }
  }

  private void markSeriesToIgnore(Series series) {
    series.ignoreTVDB.changeValue(true);
    series.commit(_db);
  }

  private boolean shouldAcceptMismatch(DBObject errorLog) {
    if (!isMismatchError(errorLog)) {
      return false;
    }

    String chosenName = (String) errorLog.get("ChosenName");

    return chosenName != null && !"".equals(chosenName);
  }

  private void resolveError(DBObject errorLog) {
    BasicDBObject queryObject = new BasicDBObject("_id", errorLog.get("_id"));

    BasicDBObject updateObject = new BasicDBObject()
        .append("Resolved", true)
        .append("ResolvedDate", new Date());

    updateCollectionWithQuery("errorlogs", queryObject, updateObject);
  }

  private void updateShowData(Series series) {
    Integer tvdbID = series.tvdbId.getValue();
    String tivoSeriesId = series.seriesId.getValue();
    String seriesTitle = series.seriesTitle.getValue();
    ObjectId seriesId = series._id.getValue();

    String apiKey = "04DBA547465DC136";
    String url = "http://thetvdb.com/api/" + apiKey + "/series/" + tvdbID + "/all/en.xml";

    Document document;
    try {
      document = readXMLFromUrl(url);
    } catch (SAXException | IOException e) {
      e.printStackTrace();
      addErrorLog(tivoSeriesId, "Error calling API for TVDB ID " + tvdbID);
      return;
    }

    debug(seriesTitle + ": Data found, updating.");

    NodeList nodeList = document.getChildNodes();

    NodeList dataNode = getNodeWithTag(nodeList, "Data").getChildNodes();
    NodeList seriesNode = getNodeWithTag(dataNode, "Series").getChildNodes();


    String tvdbSeriesName = getValueOfSimpleStringNode(seriesNode, "seriesname");

    series.tvdbId.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "id"));
    series.tvdbName.changeValueFromString(tvdbSeriesName);
    series.tvdbAirsDayOfWeek.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "airs_dayofweek"));
    series.tvdbAirsTime.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "airs_time"));
    series.tvdbFirstAired.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "firstaired"));
    series.tvdbGenre.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "genre"));
    series.tvdbNetwork.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "network"));
    series.tvdbOverview.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "overview"));
    series.tvdbRating.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "rating"));
    series.tvdbRatingCount.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "ratingcount"));
    series.tvdbRuntime.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "runtime"));
    series.tvdbSeriesId.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "SeriesID"));
    series.tvdbStatus.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "status"));
    series.tvdbPoster.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "poster"));
    series.tvdbBanner.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "banner"));
    series.tvdbLastUpdated.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "lastupdated"));
    series.imdbId.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "IMDB_ID"));
    series.zap2it_id.changeValueFromString(getValueOfSimpleStringNode(seriesNode, "zap2it_id"));

    series.commit(_db);
    seriesUpdates++;

    Integer seriesEpisodesAdded = 0;
    Integer seriesEpisodesUpdated = 0;

    List<Node> episodes = getAllNodesWithTag(dataNode, "Episode");


    for (Node episodeParent : episodes) {
      NodeList episodeNode = episodeParent.getChildNodes();
      Integer tvdbEpisodeId = Integer.valueOf(getValueOfSimpleStringNode(episodeNode, "id"));

      String episodenumber = getValueOfSimpleStringNode(episodeNode, "episodenumber");
      String episodename = getValueOfSimpleStringNode(episodeNode, "episodename");
      String seasonnumber = getValueOfSimpleStringNode(episodeNode, "seasonnumber");
      String firstaired = getValueOfSimpleStringNode(episodeNode, "firstaired");

      DBObject existingEpisodeObj = findSingleMatch("episodes", "tvdbEpisodeId", tvdbEpisodeId);
      Boolean matched = false;
      Boolean added = false;

      Date showingStartTime = null;

      Episode episode = new Episode();

      if (existingEpisodeObj == null) {
        // todo: Optimization: skip looking for match when firstAired is future. Obviously it's not on the TiVo yet.
        Episode tiVoMatch = findTiVoMatch(episodename, seasonnumber, episodenumber, firstaired, seriesId);
        if (tiVoMatch == null) {
          episode.initializeForInsert();
          added = true;
        } else {
          episode = tiVoMatch;
          matched = true;
          showingStartTime = episode.tiVoShowingStartTime.getValue();
        }
      } else {
        episode.initializeFromDBObject(existingEpisodeObj);
      }

      // todo: Add log entry for when TVDB values change.

      episode.seriesId.changeValue(seriesId);
      episode.tivoSeriesId.changeValueFromString(tivoSeriesId);
      episode.tivoSeriesTitle.changeValueFromString(seriesTitle);
      episode.tvdbSeriesName.changeValueFromString(tvdbSeriesName);
      episode.tvdbEpisodeId.changeValue(tvdbEpisodeId);
      episode.tvdbAbsoluteNumber.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "absoute_number"));
      episode.tvdbSeason.changeValueFromString(seasonnumber);
      episode.tvdbEpisodeNumber.changeValueFromString(episodenumber);
      episode.tvdbEpisodeName.changeValueFromString(episodename);
      episode.tvdbFirstAired.changeValueFromString(firstaired);
      episode.tvdbOverview.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "overview"));
      episode.tvdbProductionCode.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "ProductionCode"));
      episode.tvdbRating.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "Rating"));
      episode.tvdbRatingCount.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "RatingCount"));
      episode.tvdbDirector.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "Director"));
      episode.tvdbWriter.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "Writer"));
      episode.tvdbLastUpdated.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "lastupdated"));
      episode.tvdbSeasonId.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "seasonid"));
      episode.tvdbFilename.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "filename"));
      episode.tvdbAirsAfterSeason.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "airsafter_season"));
      episode.tvdbAirsBeforeSeason.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "airsbefore_season"));
      episode.tvdbAirsBeforeEpisode.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "airsbefore_episode"));
      episode.tvdbThumbHeight.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "thumb_height"));
      episode.tvdbThumbWidth.changeValueFromString(getValueOfSimpleStringNode(episodeNode, "thumb_width"));


      episode.commit(_db);

      if (added) {
        episodesAdded++;
        seriesEpisodesAdded++;
      } else {
        episodesUpdated++;
        seriesEpisodesUpdated++;
      }

      ObjectId episodeId = episode._id.getValue();

      if (episodeId == null) {
        throw new RuntimeException("_id wasn't populated on Episode with tvdbEpisodeId " + tvdbEpisodeId + " after insert.");
      } else {
        // add manual reference to episode to episodes array.

        series.episodes.addToArray(episodeId);
        updateSeriesDenorms(added, matched, series, showingStartTime);

        series.commit(_db);
      }
    }

//    series = findSingleMatch(_db.getCollection("series"), "_id", seriesId);
//    verifyEpisodesArray(series);

    debug(seriesTitle + ": Update complete! Added: " + seriesEpisodesAdded + "; Updated: " + seriesEpisodesUpdated);

  }


  private void updateSeriesDenorms(Boolean added, Boolean matched, Series series, Date showingStartTime) {
    // todo: TiVoUpdater should initialize all these to 0
    if (added) {
      series.tvdbOnlyEpisodes.increment(1);
      series.unwatchedUnrecorded.increment(1);
    }
    if (matched) {
      series.matchedEpisodes.increment(1);
      series.unmatchedEpisodes.increment(-1);
      series.activeEpisodes.increment(1);
      series.unwatchedEpisodes.increment(1);

      Date lastUnwatched = series.lastUnwatched.getValue();
      Date mostRecent = series.mostRecent.getValue();

      if (lastUnwatched == null || lastUnwatched.before(showingStartTime)) {
        series.lastUnwatched.changeValue(showingStartTime);
      }
      if (mostRecent == null || mostRecent.before(showingStartTime)) {
        series.mostRecent.changeValue(showingStartTime);
      }
    }
  }


  private void verifyEpisodesArray(DBObject series) {

    List<Object> arrayNotCollection = new ArrayList<>();
    List<Object> collectionNotArray = new ArrayList<>();

    DBCollection episodesCollection = _db.getCollection("episodes");

    BasicDBList episodeArray = (BasicDBList) series.get("episodes");

    DBCursor cursor = episodesCollection.find(new BasicDBObject("SeriesId", series.get("_id")));
    List<Object> collectionIds = new ArrayList<>();

    while (cursor.hasNext()) {
      DBObject episode = cursor.next();

      Object episodeId = episode.get("_id");
      collectionIds.add(episodeId);

      if (!episodeArray.contains(episodeId)) {
        collectionNotArray.add(episodeId);
      }
    }

    for (Object episodeId : episodeArray) {
      if (!collectionIds.contains(episodeId)) {
        arrayNotCollection.add(episodeId);
      }
    }

    if (!arrayNotCollection.isEmpty() || !collectionNotArray.isEmpty()) {
      String errorMessage = "Episodes array doesn't match episode ids found in collection: ";
      if (!arrayNotCollection.isEmpty()) {
        errorMessage += "Not in collection: {" + arrayNotCollection + "}. ";
      }
      if (!collectionNotArray.isEmpty()) {
        errorMessage += "Not in array: {" + collectionNotArray + "}. ";
      }
      debug(errorMessage);
    }
  }


  // todo: Handle finding two TiVo matches.
  private Episode findTiVoMatch(String episodeTitle, String tvdbSeasonStr, String tvdbEpisodeNumberStr, String firstAiredStr, Object seriesId) {
    List<Episode> matchingEpisodes = new ArrayList<>();

    DBCursor cursor = _db.getCollection("episodes")
        .find(new BasicDBObject()
                .append("SeriesId", seriesId)
                .append("tvdbEpisodeId", null)
        );

    List<Episode> episodes = new ArrayList<>();

    while(cursor.hasNext()) {
      Episode episode = new Episode();
      episode.initializeFromDBObject(cursor.next());
      episodes.add(episode);
    }

    if (episodeTitle != null) {
      for (Episode episode : episodes) {
        String tivoTitleObject = episode.tivoEpisodeTitle.getValue();
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

      for (Episode episode : episodes) {

        Integer tivoEpisodeNumber = episode.tiVoEpisodeNumber.getValue();

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

      for (Episode episode : episodes) {
        Date showingStartTimeObj = episode.tiVoShowingStartTime.getValue();

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



  private void addShowNotFoundErrorLog(Series series, String formattedName, String context) {


    BasicDBObject object = new BasicDBObject()
        .append("TiVoName", series.tivoName.getValue())
        .append("FormattedName", formattedName)
        .append("Context", context)
        .append("ErrorType", "NoMatchFound")
        .append("ErrorMessage", "Unable to find TVDB show with TiVo Name.");

    addBasicErrorLog(series.seriesId.getValue(), object);
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
  }

  private void addErrorLog(String tivoId, String errorMessage) {
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
  }


  private Node getNodeWithTag(NodeList nodeList, String tag) {
    for (int x = 0; x < nodeList.getLength(); x++) {
      Node item = nodeList.item(x);
      if (tag.equalsIgnoreCase(item.getNodeName())) {
        return item;
      }
    }
    return null;
  }

  private List<Node> getAllNodesWithTag(NodeList nodeList, String tag) {
    List<Node> matchingNodes = new ArrayList<>();
    for (int x = 0; x < nodeList.getLength(); x++) {
      Node item = nodeList.item(x);
      if (tag.equals(item.getNodeName())) {
        matchingNodes.add(item);
      }
    }
    return matchingNodes;
  }

  private String getValueOfSimpleStringNode(NodeList nodeList, String tag) {
    Node nodeWithTag = getNodeWithTag(nodeList, tag);
    return nodeWithTag == null ? null : parseSimpleStringFromNode(nodeWithTag);
  }

  private String parseSimpleStringFromNode(Node nodeWithTag) {
    NodeList childNodes = nodeWithTag.getChildNodes();
    if (childNodes.getLength() > 1) {
      throw new RuntimeException("Expect only one text child of node '" + nodeWithTag.getNodeName() + "'");
    } else if (childNodes.getLength() == 0) {
      return null;
    }
    Node textNode = childNodes.item(0);
    return textNode.getNodeValue();
  }


  public BasicDBObject getSessionInfo() {
    return new BasicDBObject()
    .append("TVDBSeriesUpdates", seriesUpdates)
    .append("TVDBEpisodesUpdated", episodesUpdated)
    .append("TVDBEpisodesAdded", episodesAdded);
  }
}

