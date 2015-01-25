package com.mayhew3.gamesutil;

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
    ObjectId seriesId = (ObjectId) show.get("_id");
    String seriesTitle = (String) show.get("SeriesTitle");
    String tivoId = (String) show.get("SeriesId");

    DBObject errorLog = getErrorLog(tivoId);

    if (shouldIgnoreShow(errorLog)) {
      markSeriesToIgnore(seriesId);
      resolveError(errorLog);
    } else {

      Object existingObj = show.get("tvdbId");

      Integer tvdbId = existingObj == null ?
          getTVDBID(show, errorLog) :
          Integer.valueOf((String) existingObj);

      if (tvdbId != null) {
        debug(seriesTitle + ": ID found, getting show data.");

        updateShowData(tivoId, tvdbId, seriesId, seriesTitle);

      }
    }
  }

  private DBObject getErrorLog(String tivoId) {
    BasicDBObject query = new BasicDBObject("TiVoID", tivoId)
        .append("Resolved", false);
    return _db.getCollection("errorlogs").findOne(query);
  }

  private Integer getTVDBID(DBObject show, DBObject errorLog) {
    String seriesTitle = (String) show.get("SeriesTitle");
    String tivoId = (String) show.get("SeriesId");
    String tvdbHint = (String) show.get("TVDBHint");

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
      addShowNotFoundErrorLog(tivoId, seriesTitle, formattedTitle, "HTTP Timeout");
      return null;
    }

    NodeList nodeList = document.getChildNodes();

    NodeList dataNode = getNodeWithTag(nodeList, "Data").getChildNodes();

    List<Node> seriesNodes = getAllNodesWithTag(dataNode, "Series");

    if (seriesNodes.isEmpty()) {
      debug("Show not found!");
      if (!isNotFoundError(errorLog)) {
        addShowNotFoundErrorLog(tivoId, seriesTitle, formattedTitle, "Empty result found.");
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

  private void markSeriesToIgnore(ObjectId seriesId) {
    BasicDBObject queryObject = new BasicDBObject("_id", seriesId);

    BasicDBObject ignoreTVDB = new BasicDBObject("IgnoreTVDB", true);

    updateCollectionWithQuery("series", queryObject, ignoreTVDB);
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

  private void updateShowData(String tivoId, Integer tvdbID, ObjectId seriesId, String seriesTitle) {
    String apiKey = "04DBA547465DC136";
    String url = "http://thetvdb.com/api/" + apiKey + "/series/" + tvdbID + "/all/en.xml";

    Document document;
    try {
      document = readXMLFromUrl(url);
    } catch (SAXException | IOException e) {
      e.printStackTrace();
      addErrorLog(tivoId, "Error calling API for TVDB ID " + tvdbID);
      return;
    }

    debug(seriesTitle + ": Data found, updating.");

    NodeList nodeList = document.getChildNodes();

    NodeList dataNode = getNodeWithTag(nodeList, "Data").getChildNodes();
    NodeList seriesNode = getNodeWithTag(dataNode, "Series").getChildNodes();


    String genreFullString = getValueOfSimpleStringNode(seriesNode, "genre");

    String[] genres = genreFullString == null ? new String[]{} :
        genreFullString
        .replaceFirst("^\\|", "")
        .split("\\|");

    String tvdbSeriesName = getValueOfSimpleStringNode(seriesNode, "seriesname");
    BasicDBObject seriesUpdate = new BasicDBObject()
        .append("tvdbId", getValueOfSimpleStringNode(seriesNode, "id"))
        .append("tvdbName", tvdbSeriesName)
        .append("tvdbAirsDayOfWeek", getValueOfSimpleStringNode(seriesNode, "airs_dayofweek"))
        .append("tvdbAirsTime", getValueOfSimpleStringNode(seriesNode, "airs_time"))
        .append("tvdbFirstAired", getValueOfSimpleStringNode(seriesNode, "firstaired"))
        .append("tvdbGenre", genres)
        .append("tvdbNetwork", getValueOfSimpleStringNode(seriesNode, "network"))
        .append("tvdbOverview", getValueOfSimpleStringNode(seriesNode, "overview"))
        .append("tvdbRating", getValueOfSimpleStringNode(seriesNode, "rating"))
        .append("tvdbRatingCount", getValueOfSimpleStringNode(seriesNode, "ratingcount"))
        .append("tvdbRuntime", getValueOfSimpleStringNode(seriesNode, "runtime"))
        .append("tvdbSeriesId", getValueOfSimpleStringNode(seriesNode, "SeriesID"))
        .append("tvdbStatus", getValueOfSimpleStringNode(seriesNode, "status"))
        .append("tvdbPoster", getValueOfSimpleStringNode(seriesNode, "poster"))
        .append("tvdbBanner", getValueOfSimpleStringNode(seriesNode, "banner"))
        .append("tvdbLastUpdated", getValueOfSimpleStringNode(seriesNode, "lastupdated"))
        .append("imdbId", getValueOfSimpleStringNode(seriesNode, "IMDB_ID"))
        .append("zap2it_id", getValueOfSimpleStringNode(seriesNode, "zap2it_id"))
        ;

    updateObjectWithId("series", seriesId, seriesUpdate);
    seriesUpdates++;

    Integer seriesEpisodesAdded = 0;
    Integer seriesEpisodesUpdated = 0;

    List<Node> episodes = getAllNodesWithTag(dataNode, "Episode");


    for (Node episode : episodes) {
      NodeList episodeNode = episode.getChildNodes();
      String tvdbEpisodeId = getValueOfSimpleStringNode(episodeNode, "id");


      DBCollection episodeCollection = _db.getCollection("episodes");


      String episodenumber = getValueOfSimpleStringNode(episodeNode, "episodenumber");
      String episodename = getValueOfSimpleStringNode(episodeNode, "episodename");
      String seasonnumber = getValueOfSimpleStringNode(episodeNode, "seasonnumber");
      String firstaired = getValueOfSimpleStringNode(episodeNode, "firstaired");



      DBObject existingEpisode = findSingleMatch("episodes", "tvdbEpisodeId", tvdbEpisodeId);
      Boolean matched = false;

      if (existingEpisode == null) {
        // todo: Optimization: skip looking for match when firstAired is future. Obviously it's not on the TiVo yet.
        existingEpisode = findTiVoMatch(episodename, seasonnumber, episodenumber, firstaired, seriesId);
        if (existingEpisode != null) {
          matched = true;
        }
      }


      // todo: loop through fields on DBObject to find changed values. Only change those values. Only run
      // todo: update if there are more than 0 changed fields. Add log entry for when TVDB values change.

      BasicDBObject tvdbObject = new BasicDBObject()
          .append("SeriesId", seriesId)
          .append("TiVoSeriesId", tivoId)
          .append("TiVoSeriesTitle", seriesTitle)
          .append("tvdbSeriesName", tvdbSeriesName)
          .append("tvdbEpisodeId", tvdbEpisodeId)
          .append("tvdbAbsoluteNumber", getValueOfSimpleStringNode(episodeNode, "absoute_number"))
          .append("tvdbSeason", seasonnumber)
          .append("tvdbEpisodeNumber", episodenumber)
          .append("tvdbEpisodeName", episodename)
          .append("tvdbFirstAired", firstaired)
          .append("tvdbOverview", getValueOfSimpleStringNode(episodeNode, "overview"))
          .append("tvdbProductionCode", getValueOfSimpleStringNode(episodeNode, "ProductionCode"))
          .append("tvdbRating", getValueOfSimpleStringNode(episodeNode, "Rating"))
          .append("tvdbRatingCount", getValueOfSimpleStringNode(episodeNode, "RatingCount"))
          .append("tvdbDirector", getValueOfSimpleStringNode(episodeNode, "Director"))
          .append("tvdbWriter", getValueOfSimpleStringNode(episodeNode, "Writer"))
          .append("tvdbLastUpdated", getValueOfSimpleStringNode(episodeNode, "lastupdated"))
          .append("tvdbSeasonId", getValueOfSimpleStringNode(episodeNode, "seasonid"))
          .append("tvdbFilename", getValueOfSimpleStringNode(episodeNode, "filename"))
          .append("tvdbAirsAfterSeason", getValueOfSimpleStringNode(episodeNode, "airsafter_season"))
          .append("tvdbAirsBeforeSeason", getValueOfSimpleStringNode(episodeNode, "airsbefore_season"))
          .append("tvdbAirsBeforeEpisode", getValueOfSimpleStringNode(episodeNode, "airsbefore_episode"))
          .append("tvdbThumbHeight", getValueOfSimpleStringNode(episodeNode, "thumb_height"))
          .append("tvdbThumbWidth", getValueOfSimpleStringNode(episodeNode, "thumb_width"));

      Object episodeId;
      Boolean added = false;

      if (existingEpisode == null) {
        tvdbObject.append("DateAdded", new Date());
        episodeCollection.insert(tvdbObject);
        added = true;
        episodesAdded++;
        seriesEpisodesAdded++;
        episodeId = tvdbObject.get("_id");
        if (episodeId == null) {
          throw new RuntimeException("_id wasn't populated on Episode with tvdbEpisodeId " + tvdbEpisodeId + " after insert.");
        }
      } else {
        episodeId = existingEpisode.get("_id");
        updateObjectWithId("episodes", episodeId, tvdbObject);
        episodesUpdated++;
        seriesEpisodesUpdated++;
      }


      if (episodeId != null) {
        // add manual reference to episode to episodes array.
        BasicDBObject queryObject = new BasicDBObject("_id", seriesId);
        BasicDBObject updateObject = new BasicDBObject("episodes", episodeId);

        _db.getCollection("series").update(queryObject, new BasicDBObject("$addToSet", updateObject));

        BasicDBObject incObject = new BasicDBObject();
        updateSeriesDenorms(added, matched, incObject);

        _db.getCollection("series").update(queryObject, new BasicDBObject("$inc", incObject));
      }
    }

//    series = findSingleMatch(_db.getCollection("series"), "_id", seriesId);
//    verifyEpisodesArray(series);

    debug(seriesTitle + ": Update complete! Added: " + seriesEpisodesAdded + "; Updated: " + seriesEpisodesUpdated);

  }


  private void updateSeriesDenorms(Boolean added, Boolean matched, BasicDBObject incObject) {

    if (added) {
      incObject.append("tvdbOnlyEpisodes", 1);
      incObject.append("UnwatchedUnrecorded", 1);
    }
    if (matched) {
      incObject.append("MatchedEpisodes", 1);
      incObject.append("UnmatchedEpisodes", -1);
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


  private DBObject findTiVoMatch(String episodeTitle, String tvdbSeasonStr, String tvdbEpisodeNumberStr, String firstAiredStr, Object seriesId) {
    List<DBObject> matchingEpisodes = new ArrayList<>();

    List<DBObject> episodes = _db.getCollection("episodes")
        .find(new BasicDBObject()
                .append("SeriesId", seriesId)
                .append("tvdbEpisodeId", null)
        ).toArray();

    if (episodeTitle != null) {
      for (DBObject episode : episodes) {
        String tivoTitleObject = (String) episode.get("TiVoEpisodeTitle");
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

      for (DBObject episode : episodes) {

        String tiVoEpisodeNumberStr = (String) episode.get("TiVoEpisodeNumber");

        if (tiVoEpisodeNumberStr != null) {

          Integer tivoEpisodeNumber = Integer.valueOf(tiVoEpisodeNumberStr);
          Integer tivoSeasonNumber = 1;

          if (tivoEpisodeNumber < 100) {
            if (Objects.equals(tivoSeasonNumber, tvdbSeason) && Objects.equals(tivoEpisodeNumber, tvdbEpisodeNumber)) {
              matchingEpisodes.add(episode);
            }
          } else {
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

      for (DBObject episode : episodes) {
        Object showingStartTimeObj = episode.get("TiVoShowingStartTime");

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



  private void addShowNotFoundErrorLog(String tivoId, String tivoName, String formattedName, String context) {
    BasicDBObject object = new BasicDBObject()
        .append("TiVoName", tivoName)
        .append("FormattedName", formattedName)
        .append("Context", context)
        .append("ErrorType", "NoMatchFound")
        .append("ErrorMessage", "Unable to find TVDB show with TiVo Name.");

    addBasicErrorLog(tivoId, object);
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

