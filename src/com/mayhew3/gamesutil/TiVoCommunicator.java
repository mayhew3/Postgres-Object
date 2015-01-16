package com.mayhew3.gamesutil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TiVoCommunicator extends TVDatabaseUtility {

  private Boolean lookAtAllShows = false;
  private List<String> episodesOnTiVo;

  private Integer addedShows = 0;
  private Integer deletedShows = 0;
  private Integer updatedShows = 0;

  private Boolean updateAllFieldsMode = false;

  public TiVoCommunicator() throws UnknownHostException {
    super("tv");
  }

  public void runUpdate(Boolean updateAllShows) {

    episodesOnTiVo = new ArrayList<>();

    // Only enable when adding fields! Will slow shit down!
    updateAllFieldsMode = false;

    lookAtAllShows = updateAllShows;

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

      updateFields();
      closeDatabase();

    } catch (RuntimeException e) {
      closeDatabase();
      throw e;
    }

  }

  public void updateFields() {
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

  private void checkForDeletedShows() {
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

  private void updateDeletedDate(String programId) {
    Date now = Calendar.getInstance().getTime();

    DBCollection collection = _db.getCollection("episodes");

    BasicDBObject query = new BasicDBObject("ProgramId", programId);

    BasicDBObject updateObject = new BasicDBObject();
    updateObject.append("$set", new BasicDBObject().append("DeletedDate", now));

    collection.update(query, updateObject);
  }

  private boolean parseShowsFromDocument(Document document) throws EpisodeAlreadyFoundException {
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

  private void parseAndUpdateSingleShow(NodeList showAttributes) throws EpisodeAlreadyFoundException {
    Map<String, String> attributeMap = Maps.newHashMap();

    attributeMap.put("CaptureDate", "TiVoCaptureDate");
    attributeMap.put("Description", "TiVoDescription");
    attributeMap.put("EpisodeTitle", "TiVoEpisodeTitle");
    attributeMap.put("EpisodeNumber", "TiVoEpisodeNumber");
    attributeMap.put("ShowingStartTime", "TiVoShowingStartTime");
    attributeMap.put("HighDefinition", "TiVoHD");
    attributeMap.put("ProgramId", "TiVoProgramId");
    attributeMap.put("Duration", "TiVoDuration");
    attributeMap.put("ShowingDuration", "TiVoShowingDuration");
    attributeMap.put("SourceChannel", "TiVoChannel");
    attributeMap.put("SourceStation", "TiVoStation");
    attributeMap.put("TvRating", "TiVoRating");

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
      DBObject seriesObject = findSingleMatch(series, "SeriesId", seriesId);

      if (seriesObject == null) {
        debug("Adding series '" + seriesTitle + "'  with ID '" + seriesId + "'");

        Boolean isEpisodic = isEpisodic(showAttributes);
        BasicDBObject seriesInsert = new BasicDBObject();

        Integer tier = isSuggestion ? 5 : 4;

        seriesInsert.append("SeriesId", seriesId);
        seriesInsert.append("SeriesTitle", seriesTitle);
        seriesInsert.append("TiVoName", seriesTitle);
        seriesInsert.append("IsEpisodic", isEpisodic);
        seriesInsert.append("IsSuggestion", isSuggestion);
        seriesInsert.append("Tier", tier);
        seriesInsert.append("DateAdded", new Date());

        DBObject episode = addNewEpisode(attributeMap, dateAttributes, url, isSuggestion, showDetails);
        BasicDBList episodeList = new BasicDBList();
        episodeList.add(episode);

        seriesInsert.append("tvdbEpisodes", episodeList);

        series.insert(seriesInsert);
        addedShows++;
      } else {
        BasicDBList tvdbEpisodes = (BasicDBList) seriesObject.get("tvdbEpisodes");
        DBObject existingTiVoEpisode = findTiVoMatch(tvdbEpisodes, programId);

        BasicDBObject queryObject = new BasicDBObject();
        queryObject.append("_id", seriesObject.get("_id"));

        BasicDBObject updateObject = new BasicDBObject();

        if (existingTiVoEpisode == null) {
          String episodeTitle = getValueOfSimpleStringNode(showDetails, "EpisodeTitle");
          String episodeNumber = getValueOfSimpleStringNode(showDetails, "EpisodeNumber");
          String showingStartTime = getValueOfSimpleStringNode(showDetails, "ShowingStartTime");

          DBObject tvdbMatch = findTVDBMatch(episodeTitle, episodeNumber, showingStartTime, tvdbEpisodes);

          if (tvdbMatch == null) {
            debug("Adding new episode '" + episodeTitle + "' to series '" + seriesTitle + "'");

            DBObject episode = addNewEpisode(attributeMap, dateAttributes, url, isSuggestion, showDetails);

            updateObject.append("tvdbEpisodes", episode);
            series.update(queryObject, new BasicDBObject("$push", updateObject));

            addedShows++;

          } else {
            Object tvdbEpisodeId = tvdbMatch.get("tvdbEpisodeId");
            if (tvdbEpisodeId == null) {
              debug("WARNING: Series '" + seriesTitle + "' has episodes with no EpisodeId!");
            } else {
              debug("Updating existing TVDB record with TiVo recording: episode '" + episodeTitle + "' of series '" + seriesTitle + "'");

              BasicDBObject episodeChanges = updateEpisode(attributeMap, dateAttributes, url, isSuggestion, showDetails, tvdbMatch);

              queryObject.append("tvdbEpisodes.tvdbEpisodeId", tvdbEpisodeId);
              series.update(queryObject, new BasicDBObject("$set", episodeChanges));

              updatedShows++;
            }
          }
        } else {
          if (updateAllFieldsMode) {
            BasicDBObject episodeChanges = updateEpisode(attributeMap, dateAttributes, url, isSuggestion, showDetails, existingTiVoEpisode);

            queryObject.append("tvdbEpisodes.TiVoProgramId", programId);
            series.update(queryObject, new BasicDBObject("$set", episodeChanges));

            updatedShows++;
          } else if (!lookAtAllShows && hasTiVoInformation(existingTiVoEpisode) && hasSameDate(existingTiVoEpisode, showDetails)) {
            debug("Found existing recording with id '" + programId + "'. Ending session.");
            throw new EpisodeAlreadyFoundException("Episode found with ID " + programId);
          }
        }
      }
    }


  }

  private Boolean hasTiVoInformation(DBObject episode) {
    return Boolean.TRUE.equals(episode.get("OnTiVo"));
  }

  private Boolean hasSameDate(DBObject existingEpisode, NodeList showDetails) {
    Date showingStartTime = getDate(getValueOfSimpleStringNode(showDetails, "ShowingStartTime"));
    if (showingStartTime == null) {
      return false;
    }
    return showingStartTime.equals(existingEpisode.get("TiVoShowingStartTime"));
  }

  private DBObject findTiVoMatch(BasicDBList episodes, String programId) {
    if (episodes == null) {
      return null;
    }
    for (Object episodeObj : episodes) {
      DBObject episode = (DBObject) episodeObj;
      if (Objects.equals(episode.get("TiVoProgramId"), programId)) {
        return episode;
      }
    }
    return null;
  }

  private DBObject findTVDBMatch(String episodeTitle, String episodeNumberStr, String startTimeStr, BasicDBList tvdbEpisodes) {
    if (tvdbEpisodes == null) {
      return null;
    }

    if (episodeTitle != null) {
      for (Object tvdbEpisode : tvdbEpisodes) {
        BasicDBObject fullObject = (BasicDBObject) tvdbEpisode;
        String tvdbTitleObject = (String) fullObject.get("tvdbEpisodeName");

        if (episodeTitle.equalsIgnoreCase(tvdbTitleObject)) {
          return fullObject;
        }
      }
    }

    // no match found on episode title. Try episode number.

    if (episodeNumberStr != null) {
      Integer episodeNumber = Integer.valueOf(episodeNumberStr);
      Integer seasonNumber = 1;

      if (episodeNumber < 100) {
        DBObject match = checkForNumberMatch(seasonNumber, episodeNumber, tvdbEpisodes);
        if (match != null) {
          return match;
        }
      } else {
        String episodeNumberString = episodeNumber.toString();
        int seasonLength = episodeNumberString.length() / 2;

        String seasonString = episodeNumberString.substring(0, seasonLength);
        String episodeString = episodeNumberString.substring(seasonLength, episodeNumberString.length());

        DBObject match = checkForNumberMatch(Integer.valueOf(seasonString), Integer.valueOf(episodeString), tvdbEpisodes);

        if (match != null) {
          return match;
        }
      }
    }

    // no match on episode number. Try air date.

    if (startTimeStr != null) {
      DateTime showingStartTime = new DateTime(getDate(startTimeStr));

      for (Object tvdbEpisode : tvdbEpisodes) {
        BasicDBObject fullObject = (BasicDBObject) tvdbEpisode;
        Object firstAiredObj = fullObject.get("tvdbFirstAired");

        if (firstAiredObj != null) {
          DateTime firstAired = new DateTime(firstAiredObj);

          DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();

          if (comparator.compare(showingStartTime, firstAired) == 0) {
            return fullObject;
          }
        }
      }

    }

    return null;
  }


  private DBObject checkForNumberMatch(Integer seasonNumber, Integer episodeNumber, BasicDBList tvdbEpisodes) {
    for (Object tvdbEpisode : tvdbEpisodes) {
      BasicDBObject fullObject = (BasicDBObject) tvdbEpisode;

      String tvdbSeasonStr = (String) fullObject.get("tvdbSeason");
      String tvdbEpisodeNumberStr = (String) fullObject.get("tvdbEpisodeNumber");

      if (tvdbSeasonStr == null || tvdbEpisodeNumberStr == null) {
        return null;
      }

      Integer tvdbSeason = Integer.valueOf(tvdbSeasonStr);
      Integer tvdbEpisodeNumber = Integer.valueOf(tvdbEpisodeNumberStr);
      if (seasonNumber.equals(tvdbSeason) && episodeNumber.equals(tvdbEpisodeNumber)) {
        return fullObject;
      }


    }
    return null;
  }


  private DBObject addNewEpisode(Map<String, String> attributeMap, List<String> dateAttributes, String url, Boolean isSuggestion, NodeList showDetails) {
    BasicDBObject episodeObject = new BasicDBObject();
    for (String fieldName : attributeMap.keySet()) {
      String fieldValue = getValueOfSimpleStringNode(showDetails, fieldName);
      if (fieldValue != null) {
        if (dateAttributes.contains(fieldName)) {
          Date date = getDate(fieldValue);
          episodeObject.append(attributeMap.get(fieldName), date);
        } else {
          episodeObject.append(attributeMap.get(fieldName), fieldValue);
        }
      }
    }
    episodeObject.append("DateAdded", new Date());
    episodeObject.append("TiVoSuggestion", isSuggestion);
    episodeObject.append("Watched", false);
    episodeObject.append("OnTiVo", true);
    if (url != null) {
      episodeObject.append("TiVoUrl", url);
    }
    return episodeObject;
  }

  private Date getDate(String fieldValue) {
    if (fieldValue == null) {
      return null;
    }
    long numberOfSeconds = Long.decode(fieldValue);
    return new Date(numberOfSeconds * 1000);
  }

  private BasicDBObject updateEpisode(Map<String, String> attributeMap, List<String> dateAttributes, String url, Boolean isSuggestion, NodeList showDetails, DBObject existingProgram) {
    BasicDBObject episodeObject = new BasicDBObject();
    for (String fieldName : attributeMap.keySet()) {
      Object existingValue = existingProgram.get(fieldName);
      if (existingValue == null) {
        String fieldValue = getValueOfSimpleStringNode(showDetails, fieldName);

        if (fieldValue != null) {
          if (dateAttributes.contains(fieldName)) {
            Date date = getDate(fieldValue);
            episodeObject.append("tvdbEpisodes.$." + attributeMap.get(fieldName), date);
          } else {
            episodeObject.append("tvdbEpisodes.$." + attributeMap.get(fieldName), fieldValue);
          }
        }
      }
    }
    if (url != null && existingProgram.get("TiVoUrl") == null) {
      episodeObject.append("tvdbEpisodes.$.TiVoUrl", url);
    }
    if (existingProgram.get("TiVoSuggestion") == null) {
      episodeObject.append("tvdbEpisodes.$.TiVoSuggestion", isSuggestion);
    }

    return episodeObject;
  }

  private Boolean isSuggestion(NodeList showAttributes) {
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

  private Boolean isEpisodic(NodeList showAttributes) {
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


  private boolean parseDetailFromDocument(Document document) throws EpisodeAlreadyFoundException {
    NodeList nodeList = document.getChildNodes();

    NodeList tvBus = getNodeWithTag(nodeList, "TvBusMarshalledStruct:TvBusEnvelope").getChildNodes();
    NodeList showing = getNodeWithTag(tvBus, "showing").getChildNodes();
    NodeList program = getNodeWithTag(showing, "program").getChildNodes();
    NodeList series = getNodeWithTag(program, "series").getChildNodes();

    String isEpisodic = getValueOfSimpleStringNode(series, "isEpisodic");
    return Boolean.parseBoolean(isEpisodic);
  }


  private Boolean isRecordingNow(NodeList showAttributes) {
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

  private String getUrl(NodeList showAttributes) {
    NodeList links = getNodeWithTag(showAttributes, "Links").getChildNodes();
    NodeList content = getNodeWithTag(links, "Content").getChildNodes();
    return getValueOfSimpleStringNode(content, "Url");
  }

  private String getDetailUrl(NodeList showAttributes) {
    NodeList links = getNodeWithTag(showAttributes, "Links").getChildNodes();
    NodeList content = getNodeWithTag(links, "TiVoVideoDetails").getChildNodes();
    return getValueOfSimpleStringNode(content, "Url");
  }


  private Node getNodeWithTag(NodeList nodeList, String tag) {
    for (int x = 0; x < nodeList.getLength(); x++) {
      Node item = nodeList.item(x);
      if (tag.equals(item.getNodeName())) {
        return item;
      }
    }
    return null;
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
        .append("AddedShows", addedShows)
        .append("DeletedShows", deletedShows)
        .append("UpdatedShows", updatedShows)
        ;
  }


  private class EpisodeAlreadyFoundException extends Exception {
    public EpisodeAlreadyFoundException(String message) {
      super(message);
    }
  }
}

