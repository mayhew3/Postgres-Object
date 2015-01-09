package com.mayhew3.gamesutil;

import com.google.common.collect.Lists;
import com.mongodb.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TiVoLibraryUpdater extends DatabaseUtility {

  private static Integer connectionID;

  private static Boolean lookAtAllShows = false;
  private static List<String> episodesOnTiVo;

  private static Integer addedShows = 0;
  private static Integer deletedShows = 0;
  private static Integer updatedShows = 0;

  private static Boolean updateAllFieldsMode = false;

  public static void main(String[] args) {


    episodesOnTiVo = new ArrayList<>();

    // Only enable when adding fields! Will slow shit down!
    updateAllFieldsMode = false;

    List<String> argList = Lists.newArrayList(args);
    if (argList.contains("FullMode")) {
      lookAtAllShows = true;
    }

    try {
      SSLTool.disableCertificateValidation();

      /**
       * What I learned:
       * - SSL is a pain in the butt.
       * - There may be a magic combination of saving the certificate from the browser, running the keytool utility,
       *   adding the right VM parameters to recognize it, create a Keystore, Trusting something, running it through
       *   an SSL Factory and Https connection, but I was never able to get it to recognize the certificate.
       * - Because I'm only running this over the local network, I'm just disabling the validation completely, which
       *   is hacky, but seems to work. (After getting the Authorization popup to work. See DatabaseUtility#readXMLFromTivoUrl.
       */

      connect("tv");

      connectionID = findMaximumConnectionId() + 1;

      logConnectionStart();
      updateFields();
    } catch (UnknownHostException | RuntimeException e) {
      e.printStackTrace();
    } finally {
      logConnectionEnd();
      closeDatabase();
    }

  }

  private static Integer findMaximumConnectionId() {
    DBCollection connectlogs = _db.getCollection("connectlogs");
    DBCursor orderedCursor = connectlogs.find().sort(new BasicDBObject("ConnectionID", -1));
    if (orderedCursor.hasNext()) {
      DBObject maxRow = orderedCursor.next();
      return (Integer) maxRow.get("ConnectionID");
    } else {
      return 0;
    }
  }

  private static void logConnectionStart() {
    DBCollection collection = _db.getCollection("connectlogs");
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
    DBCollection collection = _db.getCollection("connectlogs");

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
        .append("AddedShows", addedShows)
        .append("DeletedShows", deletedShows)
        .append("UpdatedShows", updatedShows)
        .append("TimeConnected", diffInSeconds);

    collection.update(new BasicDBObject("ConnectionID", connectionID), new BasicDBObject("$set", updateObject));
  }

  public static void updateFields() {
    String fullURL = "https://10.0.0.2/TiVoConnect?Command=QueryContainer&Container=%2FNowPlaying&Recurse=Yes&ItemCount=50";

    try {
      Boolean keepGoing = true;
      Integer offset = 0;

      while (keepGoing) {
        debug("Downloading entries " + offset + " to " + (offset + 50) + "...");

        Document document = readXMLFromTivoUrl(fullURL + "&AnchorOffset=" + offset);

        debug("Checking against DB...");
        keepGoing = parseShowsFromDocument(document);
        offset += 50;
      }
    } catch (SAXException | IOException e) {
      debug("Error reading from URL: " + fullURL);
      e.printStackTrace();
    } catch (EpisodeAlreadyFoundException e) {
      debug(e.getLocalizedMessage());
      debug("Execution found episode already in database. Stopping.");
    }

    if (lookAtAllShows) {
      checkForDeletedShows();
      // todo: delete TiVo suggestions from DB completely if they're deleted. Don't need that noise.
    }

    debug("Finished.");
  }

  private static void checkForDeletedShows() {
    DBCollection episodes = _db.getCollection("episodes");

    BasicDBObject deletedDate = new BasicDBObject("DeletedDate", null);
    DBCursor dbObjects = episodes.find(deletedDate);

    while (dbObjects.hasNext()) {
      DBObject episode = dbObjects.next();
      String programId = (String) episode.get("ProgramId");


      if (!episodesOnTiVo.contains(programId)) {
        Date captureDate = (Date) episode.get("CaptureDate");
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(captureDate);
        debug("Found episode in DB that is no longer on Tivo: '" + episode.get("Title") + "' on " + formattedDate + ". Updating deletion date.");
        updateDeletedDate(programId);
        deletedShows++;
      }
    }
  }

  private static void updateDeletedDate(String programId) {
    Date now = Calendar.getInstance().getTime();

    DBCollection collection = _db.getCollection("episodes");

    BasicDBObject query = new BasicDBObject("ProgramId", programId);

    BasicDBObject updateObject = new BasicDBObject();
    updateObject.append("$set", new BasicDBObject().append("DeletedDate", now));

    collection.update(query, updateObject);
  }

  private static boolean parseShowsFromDocument(Document document) throws EpisodeAlreadyFoundException {
    NodeList nodeList = document.getChildNodes();

    Node tivoContainer = getNodeWithTag(nodeList, "TiVoContainer");
    NodeList childNodes = tivoContainer.getChildNodes();

    Integer showNumber = 0;
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node item = childNodes.item(i);
      if (item.getNodeName().equals("Item")) {
        NodeList showAttributes = item.getChildNodes();
        parseAndUpdateSingleShow(showAttributes);
        showNumber++;
      }
    }

    return showNumber == 50;
  }

  private static void parseAndUpdateSingleShow(NodeList showAttributes) throws EpisodeAlreadyFoundException {
    List<String> attributesToSave = Lists.newArrayList("Title", "SourceSize", "Duration", "CaptureDate", "ShowingDuration", "StartPadding",
        "EndPadding", "ShowingStartTime", "EpisodeTitle",
        "Description", "SourceChannel", "SourceStation", "HighDefinition", "ProgramId", "SeriesId", "EpisodeNumber",
        "StreamingPermission", "TvRating", "ShowingBits", "IdGuideSource", "SourceType");
    List<String> dateAttributes = Lists.newArrayList("CaptureDate", "ShowingStartTime");

    Boolean recordingNow = isRecordingNow(showAttributes);

    if (recordingNow) {
      debug("Skipping episode that is currently recording.");
    } else {

      String url = getUrl(showAttributes);
      Boolean isSuggestion = isSuggestion(showAttributes);

      NodeList showDetails = getNodeWithTag(showAttributes, "Details").getChildNodes();
      String programId = getValueOfSimpleStringNode(showDetails, "ProgramId");
      String seriesId = getValueOfSimpleStringNode(showDetails, "SeriesId");
      String seriesTitle = getValueOfSimpleStringNode(showDetails, "Title");

      if (programId == null) {
        throw new RuntimeException("Episode found on TiVo with no ProgramId field!");
      }

      if (seriesId == null) {
        throw new RuntimeException("Episode found on TiVo with no SeriesId field!");
      }

      if (lookAtAllShows) {
        episodesOnTiVo.add(programId);
      }

      DBCollection series = _db.getCollection("series");
      DBCursor existingSeries = findSingleMatch(series, "SeriesId", seriesId);

      if (existingSeries == null) {
        debug("Adding series '" + seriesTitle + "'  with ID '" + seriesId + "'");

        Boolean isEpisodic = isEpisodic(showAttributes);
        BasicDBObject seriesObject = new BasicDBObject();

        Integer tier = isSuggestion ? 5 : 4;

        seriesObject.append("SeriesId", seriesId);
        seriesObject.append("SeriesTitle", seriesTitle);
        seriesObject.append("IsEpisodic", isEpisodic);
        seriesObject.append("Tier", tier);

        series.insert(seriesObject);
      } else {
        debug("Series '" + seriesTitle + "' with ID '" + seriesId + "' exists. Skipping insert.");
      }


      DBCollection episodes = _db.getCollection("episodes");

      try {
        DBCursor existingProgram = findSingleMatch(episodes, "ProgramId", programId);

        if (existingProgram == null) {
          addNewShow(attributesToSave, dateAttributes, url, isSuggestion, showDetails, episodes);
        } else {
          if (updateAllFieldsMode) {
            updateShow(attributesToSave, dateAttributes, url, isSuggestion, showDetails, programId, episodes, existingProgram);
          }
          if (!lookAtAllShows) {
            debug("Found existing recording with id '" + programId + "'. Ending session.");
            throw new EpisodeAlreadyFoundException("Episode found with ID " + programId);
          }
        }
      } catch (IllegalStateException e) {
        debug("Error updating program with id '" + programId + "'. Multiple matches found.");
        e.printStackTrace();
        throw new EpisodeAlreadyFoundException("Multiple episodes found with ID " + programId);
      }
    }

  }

  private static void addNewShow(List<String> attributesToSave, List<String> dateAttributes, String url, Boolean isSuggestion, NodeList showDetails, DBCollection episodes) {
    BasicDBObject episodeObject = new BasicDBObject();
    for (String fieldName : attributesToSave) {
      String fieldValue = getValueOfSimpleStringNode(showDetails, fieldName);
      if (fieldValue != null) {
        if (dateAttributes.contains(fieldName)) {
          long numberOfSeconds = Long.decode(fieldValue);
          Date date = new Date(numberOfSeconds * 1000);
          episodeObject.append(fieldName, date);
        } else {
          episodeObject.append(fieldName, fieldValue);
        }
      }
    }
    episodeObject.append("AddedDate", new Date());
    episodeObject.append("Suggestion", isSuggestion);
    if (url != null) {
      episodeObject.append("Url", url);
    }
    episodes.insert(episodeObject);
    addedShows++;
  }

  private static void updateShow(List<String> attributesToSave, List<String> dateAttributes, String url, Boolean isSuggestion, NodeList showDetails, String programId, DBCollection episodes, DBCursor existingProgram) {
    BasicDBObject episodeObject = new BasicDBObject();
    DBObject existingObject = existingProgram.next();
    for (String fieldName : attributesToSave) {
      Object existingValue = existingObject.get(fieldName);
      if (existingValue == null) {
        String fieldValue = getValueOfSimpleStringNode(showDetails, fieldName);

        if (fieldValue != null) {
          if (dateAttributes.contains(fieldName)) {
            long numberOfSeconds = Long.decode(fieldValue);
            Date date = new Date(numberOfSeconds * 1000);
            episodeObject.append(fieldName, date);
          } else {
            episodeObject.append(fieldName, fieldValue);
          }
        }
      }
    }
    if (url != null && existingObject.get("Url") == null) {
      episodeObject.append("Url", url);
    }
    if (existingObject.get("Suggestion") == null) {
      episodeObject.append("Suggestion", isSuggestion);
    }

    if (episodeObject.size() > 0) {
      debug(episodeObject.toString());
      episodes.update(new BasicDBObject("ProgramId", programId), new BasicDBObject("$set", episodeObject));
      updatedShows++;
    }
  }

  private static Boolean isSuggestion(NodeList showAttributes) {
    NodeList links = getNodeWithTag(showAttributes, "Links").getChildNodes();
    Node customIcon = getNodeWithTag(links, "CustomIcon");
    if (customIcon == null) {
      return false;
    } else {
      NodeList customIcons = customIcon.getChildNodes();
      String iconUrl = getValueOfSimpleStringNode(customIcons, "Url");

      return iconUrl != null && iconUrl.endsWith("suggestion-recording");
    }
  }

  private static Boolean isEpisodic(NodeList showAttributes) {
    String detailUrl = getDetailUrl(showAttributes);

    try {
      Document document = readXMLFromTivoUrl(detailUrl);

      debug("Checking against DB...");
      return parseDetailFromDocument(document);
    } catch (SAXException | IOException e) {
      debug("Error reading from URL: " + detailUrl);
      e.printStackTrace();
    } catch (EpisodeAlreadyFoundException e) {
      e.printStackTrace();
    }

    return null;
  }


  private static boolean parseDetailFromDocument(Document document) throws EpisodeAlreadyFoundException {
    NodeList nodeList = document.getChildNodes();

    NodeList tvBus = getNodeWithTag(nodeList, "TvBusMarshalledStruct:TvBusEnvelope").getChildNodes();
    NodeList showing = getNodeWithTag(tvBus, "showing").getChildNodes();
    NodeList program = getNodeWithTag(showing, "program").getChildNodes();
    NodeList series = getNodeWithTag(program, "series").getChildNodes();

    String isEpisodic = getValueOfSimpleStringNode(series, "isEpisodic");
    return Boolean.parseBoolean(isEpisodic);
  }


  private static Boolean isRecordingNow(NodeList showAttributes) {
    NodeList links = getNodeWithTag(showAttributes, "Links").getChildNodes();
    Node customIcon = getNodeWithTag(links, "CustomIcon");
    if (customIcon == null) {
      return false;
    } else {
      NodeList customIcons = customIcon.getChildNodes();
      String iconUrl = getValueOfSimpleStringNode(customIcons, "Url");

      return iconUrl != null && iconUrl.endsWith("in-progress-recording");
    }
  }

  private static String getUrl(NodeList showAttributes) {
    NodeList links = getNodeWithTag(showAttributes, "Links").getChildNodes();
    NodeList content = getNodeWithTag(links, "Content").getChildNodes();
    return getValueOfSimpleStringNode(content, "Url");
  }

  private static String getDetailUrl(NodeList showAttributes) {
    NodeList links = getNodeWithTag(showAttributes, "Links").getChildNodes();
    NodeList content = getNodeWithTag(links, "TiVoVideoDetails").getChildNodes();
    return getValueOfSimpleStringNode(content, "Url");
  }


  private static Node getNodeWithTag(NodeList nodeList, String tag) {
    for (int x = 0; x < nodeList.getLength(); x++) {
      Node item = nodeList.item(x);
      if (tag.equals(item.getNodeName())) {
        return item;
      }
    }
    return null;
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

