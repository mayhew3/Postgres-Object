package com.mayhew3.gamesutil;

import com.mongodb.*;
import org.bson.types.ObjectId;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TVDBRedoUpdater extends DatabaseUtility {


  public static void main(String[] args) {

    try {
      connect("tv");

      updateUntaggedShows();
    } catch (UnknownHostException | RuntimeException e) {
      e.printStackTrace();
    } finally {
      closeDatabase();
    }

  }

  private static void updateUntaggedShows() {
    BasicDBObject query = new BasicDBObject()
//        .append("tvdbId", new BasicDBObject("$exists", false))
        .append("IsSuggestion", false)
        .append("IgnoreTVDB", new BasicDBObject("$ne", true))
        .append("SeriesId", new BasicDBObject("$exists", true))
        .append("IsEpisodic", true);

    DBCollection untaggedShows = _db.getCollection("series");
    DBCursor cursor = untaggedShows.find(query);

    int totalRows = cursor.size();
    debug(totalRows + " series found for update. Starting.");

    int i = 0;

    while (cursor.hasNext()) {
      i++;
      DBObject show = cursor.next();

      updateShow(show);

      debug(i + " out of " + totalRows + " processed.");
    }
  }

  private static void updateShow(DBObject show) {
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
          getTVDBID(tivoId, seriesTitle, errorLog) :
          Integer.valueOf((String) existingObj);

      if (tvdbId != null) {
        debug(seriesTitle + ": ID found, getting show data.");

//        deleteEpisodeData(seriesId);

        DBObject showData = getShowData(tivoId, tvdbId);

        if (showData != null) {
          debug(seriesTitle + ": Data found, updating.");
          BasicDBObject queryObject = new BasicDBObject("_id", seriesId);
          updateCollectionWithQuery("series", queryObject, showData);
          debug(seriesTitle + ": Update complete.");
        }
      }
    }
  }

  private static void deleteEpisodeData(ObjectId seriesId) {
    singleFieldUpdateWithId("series", seriesId, "tvdbEpisodes", new BasicDBList());
  }

  private static DBObject getErrorLog(String tivoId) {
    BasicDBObject query = new BasicDBObject("TiVoID", tivoId)
        .append("Resolved", false);
    return _db.getCollection("errorlogs").findOne(query);
  }

  private static Integer getTVDBID(String tivoId, String seriesTitle, DBObject errorLog) {
    String titleToCheck = getTitleToCheck(seriesTitle, errorLog);

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

  private static String getTitleToCheck(String seriesTitle, DBObject errorLog) {
    if (errorLog != null && isNotFoundError(errorLog)) {
      Object chosenName = errorLog.get("ChosenName");
      if (chosenName == null) {
        return seriesTitle;
      }
      return (String) chosenName;
    }
    return seriesTitle;
  }

  private static boolean isNotFoundError(DBObject errorLog) {
    return errorLog != null && "NoMatchFound".equals(errorLog.get("ErrorType"));
  }

  private static boolean isMismatchError(DBObject errorLog) {
    return errorLog != null && "NameMismatch".equals(errorLog.get("ErrorType"));
  }

  private static boolean shouldIgnoreShow(DBObject errorLog) {
    return errorLog != null && Boolean.TRUE.equals(errorLog.get("IgnoreError"));
  }

  private static void updateSeriesTitle(String tivoId, String seriesTitle, DBObject errorLog) {
    String chosenName = (String) errorLog.get("ChosenName");
    if (!seriesTitle.equalsIgnoreCase(chosenName)) {
      BasicDBObject queryObject = new BasicDBObject("SeriesId", tivoId);
      BasicDBObject updateObject = new BasicDBObject("SeriesTitle", chosenName);

      updateCollectionWithQuery("series", queryObject, updateObject);
    }
  }

  private static void markSeriesToIgnore(ObjectId seriesId) {
    BasicDBObject queryObject = new BasicDBObject("_id", seriesId);

    BasicDBObject ignoreTVDB = new BasicDBObject("IgnoreTVDB", true);

    updateCollectionWithQuery("series", queryObject, ignoreTVDB);
  }

  private static boolean shouldAcceptMismatch(DBObject errorLog) {
    if (!isMismatchError(errorLog)) {
      return false;
    }

    String chosenName = (String) errorLog.get("ChosenName");

    return chosenName != null && !"".equals(chosenName);
  }

  private static void resolveError(DBObject errorLog) {
    BasicDBObject queryObject = new BasicDBObject("_id", errorLog.get("_id"));

    BasicDBObject updateObject = new BasicDBObject()
        .append("Resolved", true)
        .append("ResolvedDate", new Date());

    updateCollectionWithQuery("errorlogs", queryObject, updateObject);
  }

  private static DBObject getShowData(String tivoId, Integer tvdbID) {
    String apiKey = "04DBA547465DC136";
    String url = "http://thetvdb.com/api/" + apiKey + "/series/" + tvdbID + "/all/en.xml";

    Document document;
    try {
      document = readXMLFromUrl(url);
    } catch (SAXException | IOException e) {
      e.printStackTrace();
      addErrorLog(tivoId, "Error calling API for TVDB ID " + tvdbID);
      return null;
    }

    NodeList nodeList = document.getChildNodes();

    NodeList dataNode = getNodeWithTag(nodeList, "Data").getChildNodes();
    NodeList seriesNode = getNodeWithTag(dataNode, "Series").getChildNodes();


    String genreFullString = getValueOfSimpleStringNode(seriesNode, "genre");

    String[] genres = genreFullString == null ? new String[]{} :
        genreFullString
        .replaceFirst("^\\|", "")
        .split("\\|");

    BasicDBObject seriesUpdate = new BasicDBObject()
        .append("tvdbId", getValueOfSimpleStringNode(seriesNode, "id"))
        .append("tvdbName", getValueOfSimpleStringNode(seriesNode, "seriesname"))
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

    List<Node> episodes = getAllNodesWithTag(dataNode, "Episode");
    BasicDBObject[] episodeObjects = new BasicDBObject[episodes.size()];

    for (int i = 0; i < episodes.size(); i++) {
      NodeList episodeNode = episodes.get(i).getChildNodes();
      episodeObjects[i] = new BasicDBObject()
          .append("tvdbEpisodeId", getValueOfSimpleStringNode(episodeNode, "id"))
          .append("tvdbAbsoluteNumber", getValueOfSimpleStringNode(episodeNode, "absoute_number"))
          .append("tvdbSeason", getValueOfSimpleStringNode(episodeNode, "seasonnumber"))
          .append("tvdbEpisodeNumber", getValueOfSimpleStringNode(episodeNode, "episodenumber"))
          .append("tvdbEpisodeName", getValueOfSimpleStringNode(episodeNode, "episodename"))
          .append("tvdbFirstAired", getValueOfSimpleStringNode(episodeNode, "firstaired"))
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
          .append("tvdbThumbWidth", getValueOfSimpleStringNode(episodeNode, "thumb_width"))
          ;
    }

    return seriesUpdate.append("tvdbEpisodes", episodeObjects);

  }

  private static void addShowNotFoundErrorLog(String tivoId, String tivoName, String formattedName, String context) {
    BasicDBObject object = new BasicDBObject()
        .append("TiVoName", tivoName)
        .append("FormattedName", formattedName)
        .append("Context", context)
        .append("ErrorType", "NoMatchFound")
        .append("ErrorMessage", "Unable to find TVDB show with TiVo Name.");

    addBasicErrorLog(tivoId, object);
  }

  private static void addMismatchErrorLog(String tivoId, String tivoName, String formattedName, String tvdbName) {
    BasicDBObject object = new BasicDBObject()
        .append("TiVoName", tivoName)
        .append("FormattedName", formattedName)
        .append("TVDBName", tvdbName)
        .append("ErrorType", "NameMismatch")
        .append("ErrorMessage", "Mismatch between TiVo and TVDB names.");

    addBasicErrorLog(tivoId, object);
  }

  private static void addBasicErrorLog(String tivoId, BasicDBObject errorObject) {
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

  private static void addErrorLog(String tivoId, String errorMessage) {
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


  private static Node getNodeWithTag(NodeList nodeList, String tag) {
    for (int x = 0; x < nodeList.getLength(); x++) {
      Node item = nodeList.item(x);
      if (tag.equalsIgnoreCase(item.getNodeName())) {
        return item;
      }
    }
    return null;
  }

  private static List<Node> getAllNodesWithTag(NodeList nodeList, String tag) {
    List<Node> matchingNodes = new ArrayList<>();
    for (int x = 0; x < nodeList.getLength(); x++) {
      Node item = nodeList.item(x);
      if (tag.equals(item.getNodeName())) {
        matchingNodes.add(item);
      }
    }
    return matchingNodes;
  }

  private static String getValueOfSimpleStringNode(NodeList nodeList, String tag) {
    Node nodeWithTag = getNodeWithTag(nodeList, tag);
    return nodeWithTag == null ? null : parseSimpleStringFromNode(nodeWithTag);
  }

  private static String parseSimpleStringFromNode(Node nodeWithTag) {
    NodeList childNodes = nodeWithTag.getChildNodes();
    if (childNodes.getLength() > 1) {
      throw new RuntimeException("Expect only one text child of node '" + nodeWithTag.getNodeName() + "'");
    } else if (childNodes.getLength() == 0) {
      return null;
    }
    Node textNode = childNodes.item(0);
    return textNode.getNodeValue();
  }


}

