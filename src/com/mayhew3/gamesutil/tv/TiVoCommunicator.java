package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.SSLTool;
import com.mayhew3.gamesutil.dataobject.EpisodeMongo;
import com.mayhew3.gamesutil.dataobject.FieldValue;
import com.mayhew3.gamesutil.dataobject.SeriesMongo;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    String fullURL = "https://10.0.0.14/TiVoConnect?Command=QueryContainer&Container=%2FNowPlaying&Recurse=Yes&ItemCount=50";

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

      if (lookAtAllShows) {
        checkForDeletedShows();
        // todo: delete TiVo suggestions from DB completely if they're deleted. Don't need that noise.
      }

      debug("Finished.");

    } catch (SAXException | IOException e) {
      debug("Error reading from URL: " + fullURL);
      e.printStackTrace();
    }
  }

  private void checkForDeletedShows() {
    DBCollection episodes = _db.getCollection("episodes");

    BasicDBObject deletedDate = new BasicDBObject("TiVoDeletedDate", null)
            .append("OnTiVo", true);
    DBCursor dbObjects = episodes.find(deletedDate);

    while (dbObjects.hasNext()) {
      DBObject episodeObj = dbObjects.next();

      EpisodeMongo episode = new EpisodeMongo();
      episode.initializeFromDBObject(episodeObj);

      try {
        deleteIfGone(episode);
      } catch (ShowFailedException e) {
        debug(e.getLocalizedMessage());
        e.printStackTrace();
      }
    }

  }

  private void deleteIfGone(EpisodeMongo episode) throws ShowFailedException {
    String programId = episode.tivoProgramId.getValue();

    if (programId == null) {
      throw new ShowFailedException("Episode found with OnTiVo 'true' and TiVoProgramId 'null': " + episode);
    }

    if (!episodesOnTiVo.contains(programId)) {
      Date showingTime = episode.tivoShowingStartTime.getValue();

      if (showingTime == null) {
        debug("Found episode in DB that is no longer on Tivo: '" + episode.tivoEpisodeTitle.getValue() + "' on UNKNOWN DATE. Updating deletion date.");
      } else {
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(showingTime);
        debug("Found episode in DB that is no longer on Tivo: '" + episode.tivoEpisodeTitle.getValue() + "' on " + formattedDate + ". Updating deletion date.");
      }

      episode.tivoDeletedDate.changeValue(new Date());
      episode.commit(_db);

      deletedShows++;
    }
  }

  private boolean parseShowsFromDocument(Document document) {
    NodeList nodeList = document.getChildNodes();

    Node tivoContainer = getNodeWithTag(nodeList, "TiVoContainer");
    NodeList childNodes = tivoContainer.getChildNodes();

    Boolean shouldBeLastDocument = false;

    Integer showNumber = 0;
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node item = childNodes.item(i);
      if (item.getNodeName().equals("Item")) {
        NodeList showAttributes = item.getChildNodes();
        Boolean alreadyExists = parseAndUpdateSingleShow(showAttributes);
        if (alreadyExists) {
          shouldBeLastDocument = true;
        }
        showNumber++;
      }
    }

    return !shouldBeLastDocument && showNumber == 50;
  }

  private Boolean parseAndUpdateSingleShow(NodeList showAttributes) {
    Boolean thisEpisodeExists = false;

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

      DBCollection serieses = _db.getCollection("series");
      DBObject seriesObject = findSingleMatch(serieses, "TiVoSeriesId", tivoId);

      SeriesMongo series = new SeriesMongo();

      if (seriesObject == null) {
        series.initializeForInsert();

        debug("Adding series '" + seriesTitle + "'  with TiVoID '" + tivoId + "'");

        Boolean isEpisodic = isEpisodic(showAttributes);
        Integer tier = isSuggestion ? 5 : 4;

        series.tivoSeriesId.changeValue(tivoId);
        series.seriesTitle.changeValue(seriesTitle);
        series.tivoName.changeValue(seriesTitle);
        series.isEpisodic.changeValue(isEpisodic);
        series.isSuggestion.changeValue(isSuggestion);
        series.tier.changeValue(tier);
        series.matchedWrong.changeValue(false);

        BasicDBList locations = new BasicDBList();
        locations.add("TiVo");
        series.viewingLocations.changeValue(locations);

        series.initializeDenorms();

        series.commit(_db);

        addedShows++;
      } else {
        series.initializeFromDBObject(seriesObject);

        debug("Updating existing series '" + seriesTitle + "'.");
      }

      ObjectId seriesId = series._id.getValue();

      if (series.tvdbId.getValue() == null && series.isEpisodic.getValue()) {
        TVDBSeriesUpdater updater = new TVDBSeriesUpdater(_mongoClient, _db, series);
        updater.updateSeries();
      }

      DBCursor existingEpisodes = getExistingTiVoEpisodes(programId);
      Boolean tivoEpisodeExists = existingEpisodes.hasNext();

      EpisodeMongo episode = formatEpisodeObject(url, isSuggestion, showDetails);
      episode.seriesId.changeValue(seriesId);
      episode.tivoSeriesId.changeValue(tivoId);
      episode.tivoSeriesTitle.changeValue(seriesTitle);

      Boolean added = false;
      Boolean matched = false;

      if (tivoEpisodeExists) {
        /*if (updateAllFieldsMode) {
          while (existingEpisodes.hasNext()) {
            DBObject existingEpisode = existingEpisodes.next();
            episodeId = existingEpisode.get("_id");
            updateObjectWithId("episodes", episodeId, newEpisodeObject);
            updatedShows++;
          }
        }*/
        if (!lookAtAllShows) {
          thisEpisodeExists = true;
        }
      } else {
        EpisodeMongo tvdbMatch = findTVDBEpisodeMatch(episode, seriesId);

        if (tvdbMatch == null && series.isEpisodic.getValue()) {
          TVDBSeriesUpdater updater = new TVDBSeriesUpdater(_mongoClient, _db, series);
          updater.updateSeries();

          tvdbMatch = findTVDBEpisodeMatch(episode, seriesId);
        }

        if (tvdbMatch == null) {
          added = true;

          episode.onTiVo.changeValue(true);
          episode.commit(_db);
          addedShows++;
        } else {
          matched = true;

          episode.changeToUpdateObject();
          episode = mergeEpisodes(tvdbMatch, episode);
          episode.onTiVo.changeValue(true);
          episode.commit(_db);

          updatedShows++;
        }


        ObjectId episodeId = episode._id.getValue();

        if (episodeId == null) {
          throw new RuntimeException("Episode ID should never be null after insert or update!");
        }

        if (added) {
          /*
          if (episodeId == null) {
            throw new RuntimeException("EpisodeId should never be null!");
          }*/
          series.episodes.addToArray(episodeId);
        }

        updateSeriesDenorms(episode, series, matched);

        series.commit(_db);

      }

    }

    return thisEpisodeExists;
  }


  private Boolean isAfter(Date trackingDate, Date newDate) {
    return trackingDate == null || trackingDate.before(newDate);
  }


  private void updateSeriesDenorms(EpisodeMongo episodeObject, SeriesMongo series, Boolean matched) {

    Boolean suggestion = episodeObject.tivoSuggestion.getValue();
    Date showingStartTime = episodeObject.tivoShowingStartTime.getValue();
    Boolean watched = episodeObject.watched.getValue();



    if (suggestion) {
      series.suggestionEpisodes.increment(1);
    } else {
      series.activeEpisodes.increment(1);
      series.unwatchedEpisodes.increment(1);
    }



    if (matched) {
      series.matchedEpisodes.increment(1);
      series.tvdbOnlyEpisodes.increment(-1);
      if (!watched) {
        series.unwatchedUnrecorded.increment(-1);
      }
    } else {
      series.unmatchedEpisodes.increment(1);
    }

    if (isAfter(series.mostRecent.getValue(), showingStartTime)) {
      series.mostRecent.changeValue(showingStartTime);
    }

    if (isAfter(series.lastUnwatched.getValue(), showingStartTime)) {
      series.lastUnwatched.changeValue(showingStartTime);
    }

  }

  private Boolean shouldOverrideDate(Object oldDate, Date newDate) {
    return oldDate == null || ((Date) oldDate).before(newDate);
  }

  private EpisodeMongo mergeEpisodes(EpisodeMongo existingEpisode, EpisodeMongo episodeWithFields) {
    for (FieldValue fieldValue : episodeWithFields.getAllFieldValues()) {
      Object valueToCopy = fieldValue.getValue();
      if (valueToCopy != null) {
        FieldValue matchingFieldValue = existingEpisode.getMatchingField(fieldValue);

        if (matchingFieldValue.getValue() == null) {
          matchingFieldValue.changeValue(valueToCopy);
        }
      }
    }
    return existingEpisode;
  }


  private DBCursor getExistingTiVoEpisodes(String programId) {
    DBCollection collection = _db.getCollection("episodes");

    return collection.find(new BasicDBObject("TiVoProgramId", programId));
  }

  private EpisodeMongo formatEpisodeObject(String url, Boolean isSuggestion, NodeList showDetails) {
    EpisodeMongo episode = new EpisodeMongo();
    episode.initializeForInsert();

    episode.tivoCaptureDate.changeValueFromXMLString(getValueOfSimpleStringNode(showDetails, "CaptureDate"));
    episode.tivoShowingStartTime.changeValueFromXMLString(getValueOfSimpleStringNode(showDetails, "ShowingStartTime"));

    episode.tivoDescription.changeValueFromString(getValueOfSimpleStringNode(showDetails, "Description"));
    episode.tivoEpisodeTitle.changeValueFromString(getValueOfSimpleStringNode(showDetails, "EpisodeTitle"));
    episode.tivoEpisodeNumber.changeValueFromString(getValueOfSimpleStringNode(showDetails, "EpisodeNumber"));
    episode.tivoHD.changeValueFromString(getValueOfSimpleStringNode(showDetails, "HighDefinition"));
    episode.tivoProgramId.changeValueFromString(getValueOfSimpleStringNode(showDetails, "ProgramId"));
    episode.tivoDuration.changeValueFromString(getValueOfSimpleStringNode(showDetails, "Duration"));
    episode.tivoShowingDuration.changeValueFromString(getValueOfSimpleStringNode(showDetails, "ShowingDuration"));
    episode.tivoChannel.changeValueFromString(getValueOfSimpleStringNode(showDetails, "SourceChannel"));
    episode.tivoStation.changeValueFromString(getValueOfSimpleStringNode(showDetails, "SourceStation"));
    episode.tivoRating.changeValueFromString(getValueOfSimpleStringNode(showDetails, "TvRating"));

    episode.tivoSuggestion.changeValue(isSuggestion);
    episode.watched.changeValue(false);
    episode.onTiVo.changeValue(true);
    episode.tivoUrl.changeValue(url);

    return episode;
  }

  private EpisodeMongo findTVDBEpisodeMatch(EpisodeMongo tivoEpisode, Object seriesId) {
    String episodeTitle = tivoEpisode.tivoEpisodeTitle.getValue();
    Integer episodeNumber = tivoEpisode.tivoEpisodeNumber.getValue();
    Date startTime = tivoEpisode.tivoShowingStartTime.getValue();

    DBCursor cursor = _db.getCollection("episodes")
        .find(new BasicDBObject()
                .append("SeriesId", seriesId)
                .append("TiVoProgramId", null)
        );

    List<EpisodeMongo> tvdbEpisodes = new ArrayList<>();

    while(cursor.hasNext()) {
      EpisodeMongo episode = new EpisodeMongo();
      episode.initializeFromDBObject(cursor.next());
      tvdbEpisodes.add(episode);
    }

    if (episodeTitle != null) {
      for (EpisodeMongo tvdbEpisode : tvdbEpisodes) {
        String tvdbTitleObject = tvdbEpisode.tvdbEpisodeName.getValue();

        if (episodeTitle.equalsIgnoreCase(tvdbTitleObject)) {
          return tvdbEpisode;
        }
      }
    }

    // no match found on episode title. Try episode number.

    if (episodeNumber != null) {
      Integer seasonNumber = 1;

      if (episodeNumber < 100) {
        EpisodeMongo match = checkForNumberMatch(seasonNumber, episodeNumber, tvdbEpisodes);
        if (match != null) {
          return match;
        }
      } else {
        String episodeNumberString = episodeNumber.toString();
        int seasonLength = episodeNumberString.length() / 2;

        String seasonString = episodeNumberString.substring(0, seasonLength);
        String episodeString = episodeNumberString.substring(seasonLength, episodeNumberString.length());

        EpisodeMongo match = checkForNumberMatch(Integer.valueOf(seasonString), Integer.valueOf(episodeString), tvdbEpisodes);

        if (match != null) {
          return match;
        }
      }
    }

    // no match on episode number. Try air date.

    if (startTime != null) {
      DateTime showingStartTime = new DateTime(startTime);

      for (EpisodeMongo tvdbEpisode : tvdbEpisodes) {
        Date firstAiredValue = tvdbEpisode.tvdbFirstAired.getValue();
        if (firstAiredValue != null) {
          DateTime firstAired = new DateTime(firstAiredValue);

          DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();

          if (comparator.compare(showingStartTime, firstAired) == 0) {
            return tvdbEpisode;
          }
        }
      }
    }

    return null;
  }


  private EpisodeMongo checkForNumberMatch(Integer seasonNumber, Integer episodeNumber, List<EpisodeMongo> tvdbEpisodes) {
    for (EpisodeMongo tvdbEpisode : tvdbEpisodes) {
      Integer tvdbSeason = tvdbEpisode.tvdbSeason.getValue();
      Integer tvdbEpisodeNumber = tvdbEpisode.tvdbEpisodeNumber.getValue();

      if (tvdbSeason == null || tvdbEpisodeNumber == null) {
        return null;
      }

      if (tvdbSeason.equals(seasonNumber) && tvdbEpisodeNumber.equals(episodeNumber)) {
        return tvdbEpisode;
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

