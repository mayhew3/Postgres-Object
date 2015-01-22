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

    BasicDBObject deletedDate = new BasicDBObject("TiVoDeletedDate", null)
            .append("OnTiVo", true);
    DBCursor dbObjects = episodes.find(deletedDate);

    while (dbObjects.hasNext()) {
      DBObject episode = dbObjects.next();
      deleteIfGone(episode);
    }

  }

  private void deleteIfGone(DBObject episode) {
    String programId = (String) episode.get("TiVoProgramId");

    if (programId == null) {
      throw new RuntimeException("Episode found with OnTiVo 'true' and TiVoProgramId 'null'.");
    }

    if (!episodesOnTiVo.contains(programId)) {
      Date captureDate = (Date) episode.get("TiVoCaptureDate");

      String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(captureDate);
      debug("Found episode in DB that is no longer on Tivo: '" + episode.get("TiVoEpisodeTitle") + "' on " + formattedDate + ". Updating deletion date.");

      singleFieldUpdateWithId("episodes", episode.get("_id"), "TiVoDeletedDate", Calendar.getInstance().getTime());
      deletedShows++;
    }
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
      String tivoId = getValueOfSimpleStringNode(showDetails, "SeriesId");
      String seriesTitle = getValueOfSimpleStringNode(showDetails, "Title");

      if (programId == null) {
        throw new RuntimeException("Episode found on TiVo with no ProgramId field!");
      }

      if (tivoId == null) {
        throw new RuntimeException("Episode found on TiVo with no SeriesId field!");
      }

      if (lookAtAllShows) {
        episodesOnTiVo.add(programId);
      }

      DBCollection series = _db.getCollection("series");
      DBObject seriesObject = findSingleMatch(series, "SeriesId", tivoId);
      Object seriesId;
      Object lastUnwatched = null;
      Object mostRecent = null;

      if (seriesObject == null) {
        debug("Adding series '" + seriesTitle + "'  with TiVoID '" + tivoId + "'");

        Boolean isEpisodic = isEpisodic(showAttributes);
        BasicDBObject seriesInsert = new BasicDBObject();

        Integer tier = isSuggestion ? 5 : 4;

        seriesInsert.append("SeriesId", tivoId);
        seriesInsert.append("SeriesTitle", seriesTitle);
        seriesInsert.append("TiVoName", seriesTitle);
        seriesInsert.append("IsEpisodic", isEpisodic);
        seriesInsert.append("IsSuggestion", isSuggestion);
        seriesInsert.append("Tier", tier);
        seriesInsert.append("DateAdded", new Date());

        series.insert(seriesInsert);

        seriesId = seriesInsert.get("_id");
        addedShows++;
      } else {
        debug("Updating existing series '" + seriesTitle + "'.");
        seriesId = seriesObject.get("_id");
        lastUnwatched = seriesObject.get("LastUnwatched");
        mostRecent = seriesObject.get("MostRecent");
      }

      DBCollection episodes = _db.getCollection("episodes");

      DBObject existingEpisode = getExistingTiVoEpisode(programId, episodes);
      Object episodeId = null;

      DBObject newEpisodeObject = formatEpisodeData(attributeMap, dateAttributes, url, isSuggestion, showDetails);
      newEpisodeObject.put("SeriesId", seriesId);
      newEpisodeObject.put("TiVoSeriesId", tivoId);
      newEpisodeObject.put("TiVoSeriesTitle", seriesTitle);
//      newEpisodeObject.put("TiVoProgramId", programId);

      Object tvdbEpisodeId = null;

      if (existingEpisode == null) {
        String episodeTitle = (String) newEpisodeObject.get("TiVoEpisodeTitle");
        String episodeNumber = (String) newEpisodeObject.get("TivoEpisodeNumber");
        Date showingStartTime = (Date) newEpisodeObject.get("TiVoShowingStartTime");

        DBObject tvdbMatch = findTVDBMatch(episodeTitle, episodeNumber, showingStartTime, seriesId);


        if (tvdbMatch == null) {
          _db.getCollection("episodes").insert(newEpisodeObject);
          episodeId = newEpisodeObject.get("_id");
          addedShows++;
        } else {
          episodeId = tvdbMatch.get("_id");
          tvdbEpisodeId = tvdbMatch.get("tvdbEpisodeId");
          updateObjectWithId("episodes", episodeId, newEpisodeObject);
          updatedShows++;
        }
      } else {
        if (updateAllFieldsMode) {
          episodeId = existingEpisode.get("_id");
          updateObjectWithId("episodes", episodeId, newEpisodeObject);
          updatedShows++;
        }
        if (!lookAtAllShows) {
          debug("Found existing recording with id '" + programId + "'. Ending session.");
          throw new EpisodeAlreadyFoundException("Episode found with ID " + programId);
        }
      }

      if (episodeId != null) {
        BasicDBObject queryObject = new BasicDBObject("_id", seriesId);
        BasicDBObject updateObject = new BasicDBObject("episodes", episodeId);

        _db.getCollection("series").update(queryObject, new BasicDBObject("$addToSet", updateObject));

        BasicDBObject setObject = new BasicDBObject();
        BasicDBObject incObject = new BasicDBObject();

        updateSeriesDenorms(newEpisodeObject, tvdbEpisodeId, setObject, incObject, lastUnwatched, mostRecent);

        _db.getCollection("series").update(queryObject, new BasicDBObject()
            .append("$set", setObject)
            .append("$inc", incObject));
      }


//      seriesObject = findSingleMatch(_db.getCollection("series"), "_id", seriesId);
//      verifyEpisodesArray(seriesObject);
    }

  }

  private void updateSeriesDenorms(DBObject episodeObject, Object tvdbId, BasicDBObject setObject, BasicDBObject incObject, Object lastUnwatched, Object mostRecent) {

    Object onTiVo = episodeObject.get("OnTiVo");
    Object suggestion = episodeObject.get("TiVoSuggestion");
    Object showingStartTime = episodeObject.get("TiVoShowingStartTime");
    Object deletedDate = episodeObject.get("TiVoDeletedDate");
    Object watched = episodeObject.get("Watched");

    if (Boolean.TRUE.equals(onTiVo)) {


      if (tvdbId == null) {
        incObject.append("UnmatchedEpisodes", 1);
      } else {
        incObject.append("MatchedEpisodes", 1);

        Date showingStartTimeDate = (Date) showingStartTime;
        if (deletedDate == null) {
          incObject.append("ActiveEpisodes", 1);

          if (Boolean.TRUE.equals(watched)) {
            incObject.append("WatchedEpisodes", 1);
          } else {
            incObject.append("UnwatchedEpisodes", 1);

            if (shouldOverrideDate(lastUnwatched, showingStartTimeDate)) {
              setObject.append("LastUnwatched", showingStartTimeDate);
            }
          }

          if (shouldOverrideDate(mostRecent, showingStartTimeDate)) {
            setObject.append("MostRecent", showingStartTimeDate);
          }
        } else {
          incObject.append("DeletedEpisodes", 1);
        }
        if (Boolean.TRUE.equals(suggestion)) {
          incObject.append("SuggestionEpisodes", 1);
        }
      }

    } else {
      if (tvdbId != null) {
        incObject.append("tvdbOnlyEpisodes", 1);
        if (!Boolean.TRUE.equals(watched)) {
          incObject.append("UnwatchedUnrecorded", 1);
        }
      }
    }
  }

  private Boolean shouldOverrideDate(Object oldDate, Date newDate) {
    return oldDate == null || ((Date) oldDate).before(newDate);
  }


  private DBObject getExistingTiVoEpisode(String programId, DBCollection episodes) throws EpisodeAlreadyFoundException {
    DBObject existingProgram;
    try {
      existingProgram = findSingleMatch(episodes, "TiVoProgramId", programId);
      if (existingProgram == null) {
        return findSingleMatch(episodes, "ProgramId", programId);
      }
    } catch (IllegalStateException e) {
      debug("Error updating program with id '" + programId + "'. Multiple matches found.");
      e.printStackTrace();
      throw new EpisodeAlreadyFoundException("Multiple episodes found with ID " + programId);
    }
    return existingProgram;
  }

  private DBObject formatEpisodeData(Map<String, String> attributeMap, List<String> dateAttributes, String url, Boolean isSuggestion, NodeList showDetails) {
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


  private Integer getSizeOfEpisodesArray(DBObject series) {
    Object episodes = series.get("episodes");
    if (episodes == null) {
      return 0;
    } else {
      BasicDBList dbList = (BasicDBList) episodes;
      return dbList.size();
    }
  }

  private Integer getNumberOfExistingEpisodesInCollection(DBObject series) {
    Object seriesId = series.get("_id");

    return _db.getCollection("episodes").find(new BasicDBObject("SeriesId", seriesId)).count();
  }


  private void verifyEpisodesArray(DBObject series) {
    Integer sizeOfEpisodesArray = getSizeOfEpisodesArray(series);
    Integer episodesInCollection = getNumberOfExistingEpisodesInCollection(series);

    if (sizeOfEpisodesArray.equals(episodesInCollection)) {
      if (sizeOfEpisodesArray > 0) {
        BasicDBList episodeArray = (BasicDBList) series.get("episodes");
        for (Object episodeId : episodeArray) {
          DBObject matchingEpisode = findSingleMatch(_db.getCollection("episodes"), "_id", episodeId);
          if (matchingEpisode == null) {
            throw new RuntimeException("Bad Episode Reference in episodes array: " + episodeId);
          }
        }
      }
    } else {
      debug("Size of episodes array is " + sizeOfEpisodesArray +
          " but there are " + episodesInCollection + " episodes in the collection for series " + series.get("TiVoSeriesTitle"));
    }
  }

  private Date getDate(String fieldValue) {
    if (fieldValue == null) {
      return null;
    }
    long numberOfSeconds = Long.decode(fieldValue);
    return new Date(numberOfSeconds * 1000);
  }


  private DBObject findTVDBMatch(String episodeTitle, String episodeNumberStr, Date startTime, Object seriesId) {
    List<DBObject> tvdbEpisodes = _db.getCollection("episodes").find(new BasicDBObject("SeriesId", seriesId)).toArray();

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

    if (startTime != null) {
      DateTime showingStartTime = new DateTime(startTime);

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


  private DBObject checkForNumberMatch(Integer seasonNumber, Integer episodeNumber, List<DBObject> tvdbEpisodes) {
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

