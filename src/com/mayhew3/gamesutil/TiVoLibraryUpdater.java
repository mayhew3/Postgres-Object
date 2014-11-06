package com.mayhew3.gamesutil;

import com.google.common.collect.Lists;
import com.mongodb.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TiVoLibraryUpdater extends DatabaseUtility {

  private static Boolean lookAtAllShows = false;
  private static List<String> episodesOnTiVo;

  public static void main(String[] args) {
    episodesOnTiVo = new ArrayList<>();

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
       *   is hacky, but seems to work. (After getting the Authorization popup to work. See DatabaseUtility#readXMLFromUrl.
       */

      connect("tv");

      // todo: Use a unique ConnectionID so I can easily show the connections that have been made and how long it took.
      // todo: Maybe a single row? Insert on start, update on end?
      logActivity("StartUpdate");
      updateFields();
      logActivity("EndUpdate");
      closeDatabase();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

  }

  private static void logActivity(String startUpdate) {
    DBCollection collection = _db.getCollection("connectlogs");
    BasicDBObject basicDBObject = new BasicDBObject("Type", startUpdate)
        .append("LogTime", new Date());
    try {
      collection.insert(basicDBObject);
    } catch (MongoException e) {
      throw new RuntimeException("Error inserting log into database.\r\n" + e.getLocalizedMessage());
    }
  }

  public static void updateFields() {
    String fullURL = "https://10.0.0.2/TiVoConnect?Command=QueryContainer&Container=%2FNowPlaying&Recurse=Yes&ItemCount=50";

    try {
      Boolean keepGoing = true;
      Integer offset = 0;

      while (keepGoing) {
        debug("Downloading entries " + offset + " to " + (offset + 50) + "...");
        Document document = readXMLFromUrl(fullURL + "&AnchorOffset=" + offset);

        debug("Checking against DB...");
        keepGoing = parseShowsFromDocument(document);
        offset += 50;
      }
    } catch (IOException e) {
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
    List<String> attributesToSave = Lists.newArrayList("Title", "Duration", "CaptureDate", "ShowingDuration", "ShowingStartTime", "EpisodeTitle",
        "Description", "SourceChannel", "SourceStation", "HighDefinition", "ProgramId", "SeriesId", "EpisodeNumber",
        "StreamingPermission");
    List<String> dateAttributes = Lists.newArrayList("CaptureDate", "ShowingStartTime");

    NodeList showDetails = getNodeWithTag(showAttributes, "Details").getChildNodes();
    String programId = getValueOfSimpleStringNode(showDetails, "ProgramId");

    if (programId == null) {
      throw new RuntimeException("Episode found on TiVo with no ProgramId field!");
    }

    if (lookAtAllShows) {
      episodesOnTiVo.add(programId);
    }

    DBCollection episodes = _db.getCollection("episodes");

    try {
      DBCursor existingProgram = findSingleMatch(episodes, "ProgramId", programId);

      if (existingProgram == null) {
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
        debug(episodeObject.toString());
        episodes.insert(episodeObject);
      } else {
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

