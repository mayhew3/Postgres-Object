package com.mayhew3.gamesutil;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;

public class TiVoLibraryUpdater extends DatabaseUtility {

  public static void main(String[] args) {
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
//      logActivity("StartUpdate");
      updateFields();
//      logActivity("EndUpdate");
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
        Document document = readXMLFromUrl(fullURL + "&AnchorOffset=" + offset);

        keepGoing = parseShowsFromDocument(document);
        offset += 50;
      }

    } catch (IOException e) {
      debug("Error reading from URL: " + fullURL);
      e.printStackTrace();
    }
  }

  private static boolean parseShowsFromDocument(Document document) {

    NodeList nodeList = document.getChildNodes();

    Node tivoContainer = getNodeWithTag(nodeList, "TiVoContainer");

    NodeList childNodes = tivoContainer.getChildNodes();

    Integer showNumber = 0;
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node item = childNodes.item(i);
      if (item.getNodeName().equals("Item")) {
        NodeList showAttributes = item.getChildNodes();
        parseSingleShow(showAttributes);
        showNumber++;
      }
    }

    return showNumber == 50;
  }

  private static void parseSingleShow(NodeList showAttributes) {
    String[] attributesToSave = {"Title", "Duration", "CaptureDate", "ShowingDuration", "ShowingStartTime", "EpisodeTitle",
        "Description", "SourceChannel", "SourceStation", "HighDefinition", "ProgramId", "SeriesId", "EpisodeNumber",
        "StreamingPermission"};

    Node details = getNodeWithTag(showAttributes, "Details");
    String showTitle = getValueOfSimpleStringNode(details.getChildNodes(), "Title");
    String episodeNumber = getValueOfSimpleStringNode(details.getChildNodes(), "EpisodeNumber");

    if (episodeNumber == null) {
      episodeNumber = "?x?";
    }
    String episodeTitle = getValueOfSimpleStringNode(details.getChildNodes(), "EpisodeTitle");
    if (episodeTitle == null) {
      episodeTitle = "(??)";
    }
    debug(showTitle + ": " + episodeNumber + " - '" + episodeTitle + "'");
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


}
