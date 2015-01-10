package com.mayhew3.gamesutil;

import com.google.common.collect.Lists;
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

public class TVDBUpdater extends DatabaseUtility {

  private static Integer connectionID;

  private static Boolean lookAtAllShows = false;

  private static Integer updatedShows = 0;

  private static Boolean updateAllFieldsMode = false;

  public static void main(String[] args) {



    // Only enable when adding fields! Will slow shit down!
    updateAllFieldsMode = false;

    List<String> argList = Lists.newArrayList(args);
    if (argList.contains("FullMode")) {
      lookAtAllShows = true;
    }

    try {
      connect("tv");

      connectionID = findMaximumConnectionId() + 1;

      debug(connectionID);
      updateUntaggedShows();
//      logConnectionStart();
    } catch (UnknownHostException | RuntimeException e) {
      e.printStackTrace();
    } finally {
//      logConnectionEnd();
      closeDatabase();
    }

  }

  private static Integer findMaximumConnectionId() {
    DBCollection connectlogs = _db.getCollection("tvdbconnectlogs");
    DBCursor orderedCursor = connectlogs.find().sort(new BasicDBObject("ConnectionID", -1));
    if (orderedCursor.hasNext()) {
      DBObject maxRow = orderedCursor.next();
      return (Integer) maxRow.get("ConnectionID");
    } else {
      return 0;
    }
  }

  private static void logConnectionStart() {
    DBCollection collection = _db.getCollection("tvdbconnectlogs");
    BasicDBObject basicDBObject = new BasicDBObject()
        .append("StartTime", new Date())
        .append("ConnectionID", connectionID)
        .append("FastUpdate", !lookAtAllShows);

    try {
      collection.insert(basicDBObject);
    } catch (MongoException e) {
      throw new RuntimeException("Error inserting log into database.\r\n" + e.getLocalizedMessage());
    }
  }

  private static void logConnectionEnd() {
    DBCollection collection = _db.getCollection("tvdbconnectlogs");

    DBCursor connectionLog = findSingleMatch(collection, "ConnectionID", connectionID);

    if (!connectionLog.hasNext()) {
      throw new RuntimeException("Unable to find connect log with ID " + connectionID);
    }

    DBObject existing = connectionLog.next();

    Date startTime = (Date) existing.get("StartTime");
    Date endTime = new Date();

    long diffInMillis = endTime.getTime() - startTime.getTime();

    long diffInSeconds = diffInMillis/1000;

    BasicDBObject updateObject = new BasicDBObject()
        .append("EndTime", endTime)
        .append("UpdatedShows", updatedShows)
        .append("TimeConnected", diffInSeconds);

    collection.update(new BasicDBObject("ConnectionID", connectionID), new BasicDBObject("$set", updateObject));
  }

  private static void updateUntaggedShows() {
    BasicDBObject query = new BasicDBObject()
        .append("tvdbId", new BasicDBObject("$exists", false))
        .append("IsSuggestion", false)
        .append("IgnoreTVDB", new BasicDBObject("$ne", true))
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

      Integer tvdbid = getTVDBID(tivoId, seriesTitle, errorLog);

      if (tvdbid != null) {
        debug(seriesTitle + ": ID found, getting show data.");
        DBObject showData = getShowData(tivoId, tvdbid);

        if (showData != null) {
          debug(seriesTitle + ": Data found, updating.");
          BasicDBObject queryObject = new BasicDBObject("_id", seriesId);
          _db.getCollection("series").update(queryObject, new BasicDBObject("$set", showData));
          debug(seriesTitle + ": Update complete.");
        }
      }
    }
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

      _db.getCollection("series").update(queryObject, new BasicDBObject("$set", updateObject));
    }
  }

  private static void markSeriesToIgnore(ObjectId seriesId) {
    BasicDBObject queryObject = new BasicDBObject("_id", seriesId);

    BasicDBObject ignoreTVDB = new BasicDBObject("IgnoreTVDB", true);

    _db.getCollection("series").update(queryObject, new BasicDBObject("$set", ignoreTVDB));
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

    _db.getCollection("errorlogs").update(queryObject, new BasicDBObject("$set", updateObject));
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
        .append("tvdbStatus", getValueOfSimpleStringNode(seriesNode, "status"))
        .append("tvdbPoster", getValueOfSimpleStringNode(seriesNode, "poster"))
        ;

    List<Node> episodes = getAllNodesWithTag(dataNode, "Episode");
    BasicDBObject[] episodeObjects = new BasicDBObject[episodes.size()];

    for (int i = 0; i < episodes.size(); i++) {
      NodeList episodeNode = episodes.get(i).getChildNodes();
      episodeObjects[i] = new BasicDBObject()
          .append("tvdbSeason", getValueOfSimpleStringNode(episodeNode, "seasonnumber"))
          .append("tvdbEpisodeNumber", getValueOfSimpleStringNode(episodeNode, "episodenumber"))
          .append("tvdbEpisodeName", getValueOfSimpleStringNode(episodeNode, "episodename"))
          .append("tvdbFirstAired", getValueOfSimpleStringNode(episodeNode, "firstaired"))
          .append("tvdbOverview", getValueOfSimpleStringNode(episodeNode, "overview"))
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


  private static class EpisodeAlreadyFoundException extends Exception {
    public EpisodeAlreadyFoundException(String message) {
      super(message);
    }
  }
}

