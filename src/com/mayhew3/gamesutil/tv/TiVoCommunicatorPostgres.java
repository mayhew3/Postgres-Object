package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.SSLTool;
import com.mayhew3.gamesutil.games.PostgresConnection;
import com.mayhew3.gamesutil.mediaobject.EpisodePostgres;
import com.mayhew3.gamesutil.mediaobject.SeriesPostgres;
import com.mayhew3.gamesutil.mediaobject.TVDBEpisodePostgres;
import com.mayhew3.gamesutil.mediaobject.TiVoEpisodePostgres;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TiVoCommunicatorPostgres {

  private Boolean lookAtAllShows = false;
  private List<String> episodesOnTiVo;

  private Integer addedShows = 0;
  private Integer deletedShows = 0;
  private Integer updatedShows = 0;

  private static PostgresConnection connection;

  public static void main(String[] args) throws UnknownHostException, SQLException {
    List<String> argList = Lists.newArrayList(args);
    Boolean lookAtAllShows = argList.contains("FullMode");
    Boolean dev = argList.contains("Dev");

    connection = new PostgresConnection();

    TiVoCommunicatorPostgres tiVoCommunicatorPostgres = new TiVoCommunicatorPostgres();

    if (dev) {
      tiVoCommunicatorPostgres.truncatePostgresTables();
    }

    tiVoCommunicatorPostgres.runUpdate(lookAtAllShows);
  }

  public void runUpdate(Boolean updateAllShows) throws SQLException {

    episodesOnTiVo = new ArrayList<>();
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
      connection.closeConnection();

    } catch (RuntimeException e) {
      connection.closeConnection();
      throw e;
    }

  }

  private void truncatePostgresTables() throws SQLException {
    connection.executeUpdate("TRUNCATE TABLE tvdb_series CASCADE");
    connection.executeUpdate("TRUNCATE TABLE tvdb_episode CASCADE");
    connection.executeUpdate("TRUNCATE TABLE tivo_episode CASCADE");
    connection.executeUpdate("TRUNCATE TABLE genre CASCADE");
    connection.executeUpdate("TRUNCATE TABLE viewing_location CASCADE");

    connection.executeUpdate("ALTER SEQUENCE series_id_seq RESTART WITH 1");
    connection.executeUpdate("ALTER SEQUENCE tvdb_series_id_seq RESTART WITH 1");
    connection.executeUpdate("ALTER SEQUENCE season_id_seq RESTART WITH 1");
    connection.executeUpdate("ALTER SEQUENCE episode_id_seq RESTART WITH 1");
    connection.executeUpdate("ALTER SEQUENCE tivo_episode_id_seq RESTART WITH 1");
    connection.executeUpdate("ALTER SEQUENCE tvdb_episode_id_seq RESTART WITH 1");

    connection.executeUpdate("ALTER SEQUENCE genre_id_seq RESTART WITH 1");
    connection.executeUpdate("ALTER SEQUENCE series_genre_id_seq RESTART WITH 1");

    connection.executeUpdate("ALTER SEQUENCE viewing_location_id_seq RESTART WITH 1");
    connection.executeUpdate("ALTER SEQUENCE series_viewing_location_id_seq RESTART WITH 1");

  }


  public void updateFields() throws SQLException {
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

      debug("Added: " + addedShows);
      debug("Updated: " + updatedShows);
      debug("Deleted: " + deletedShows);

    } catch (SAXException | IOException e) {
      debug("Error reading from URL: " + fullURL);
      e.printStackTrace();
    }
  }

  private void checkForDeletedShows() throws SQLException {
    ResultSet resultSet = connection.executeQuery(
        "SELECT * " +
            "FROM tivo_episode " +
            "WHERE deleted_date IS NULL"
    );

    while (connection.hasMoreElements(resultSet)) {
      TiVoEpisodePostgres tiVoEpisodePostgres = new TiVoEpisodePostgres();
      tiVoEpisodePostgres.initializeFromDBObject(resultSet);

      deleteIfGone(tiVoEpisodePostgres);
    }

  }

  private void deleteIfGone(TiVoEpisodePostgres episode) {
    String programId = episode.programId.getValue();

    if (programId == null) {
      throw new RuntimeException("TiVo Episode found with OnTiVo 'true' and TiVoProgramId 'null'.");
    }

    if (!episodesOnTiVo.contains(programId)) {
      Date showingTime = episode.showingStartTime.getValue();

      if (showingTime == null) {
        debug("Found episode in DB that is no longer on Tivo: '" + episode.title.getValue() + "' on UNKNOWN DATE. Updating deletion date.");
      } else {
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(showingTime);
        debug("Found episode in DB that is no longer on Tivo: '" + episode.title.getValue() + "' on " + formattedDate + ". Updating deletion date.");
      }

      episode.deletedDate.changeValue(new Date());
      episode.commit(connection);

      deletedShows++;
    }
  }

  private boolean parseShowsFromDocument(Document document) throws SQLException {
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

  private class TivoInfo {
    public String programId;
    public String tivoId;
    public String seriesTitle;
    public Boolean isSuggestion;
    public String url;

    public TivoInfo(@NotNull NodeList showDetails) {
      programId = getValueOfSimpleStringNode(showDetails, "ProgramId");;
      tivoId = getValueOfSimpleStringNode(showDetails, "SeriesId");
      seriesTitle = getValueOfSimpleStringNode(showDetails, "Title");

      if (programId == null) {
        throw new RuntimeException("Episode found on TiVo with no ProgramId field!");
      }

      if (tivoId == null) {
        throw new RuntimeException("Episode found on TiVo with no SeriesId field!");
      }
    }
  }

  /**
   * @param showAttributes Root XML object
   * @return Whether this episode already exists in the database, and we should stop updating later episodes in quick mode.
   * @throws SQLException
   */
  private Boolean parseAndUpdateSingleShow(NodeList showAttributes) throws SQLException {

    if (isRecordingNow(showAttributes)) {
      debug("Skipping episode that is currently recording.");
      return false;
    }

    if (!isEpisodic(showAttributes)) {
      debug("Skipping episode that is not episodic.");
      return false;
    }

    NodeList showDetails = getNodeWithTag(showAttributes, "Details").getChildNodes();

    TivoInfo tivoInfo = new TivoInfo(showDetails);
    tivoInfo.isSuggestion = isSuggestion(showAttributes);
    tivoInfo.url = getUrl(showAttributes);

    if (lookAtAllShows) {
      episodesOnTiVo.add(tivoInfo.programId);
    }

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch("SELECT * FROM series WHERE tivo_series_id = ?", tivoInfo.tivoId);

    SeriesPostgres series = new SeriesPostgres();

    if (!connection.hasMoreElements(resultSet)) {
      addNewSeries(series, tivoInfo);
    } else {
      series.initializeFromDBObject(resultSet);
      debug("Updating existing series '" + tivoInfo.seriesTitle + "'.");
    }

    Boolean tvdbUpdated = false;
    if (series.tvdbId.getValue() == null) {
      TVDBSeriesPostgresUpdater updater = new TVDBSeriesPostgresUpdater(connection, series);
      updater.updateSeries();
      tvdbUpdated = true;
    }

    return addEpisodeIfNotExists(showDetails, series, tivoInfo, tvdbUpdated);

  }


  /**
   * @return Whether the episode already exists in the database.
   * @throws SQLException
   */
  private Boolean addEpisodeIfNotExists(NodeList showDetails, SeriesPostgres series, TivoInfo tivoInfo, Boolean tvdbUpdated) throws SQLException {
    ResultSet existingEpisode = getExistingTiVoEpisodes(tivoInfo.programId);
    Boolean tivoEpisodeExists = connection.hasMoreElements(existingEpisode);

    if (tivoEpisodeExists && !lookAtAllShows) {
      return true;
    }

    TiVoEpisodePostgres tivoEpisode = getOrCreateTiVoEpisode(showDetails, tivoInfo, existingEpisode, tivoEpisodeExists);
    TVDBEpisodePostgres tvdbEpisode = findTVDBEpisodeMatch(tivoEpisode, series.id.getValue());

    if (tvdbEpisode == null) {
      tvdbEpisode = retryMatchWithUpdatedTVDB(series, tvdbUpdated, tivoEpisode);
    }

    Boolean tvdb_matched = false;


    EpisodePostgres episode = new EpisodePostgres();
    
    if (tivoEpisodeExists) {

      ResultSet existingEpisodeRow = getExistingEpisodeRow(tivoEpisode);
      episode.initializeFromDBObject(existingEpisodeRow);

    } else if (tvdbEpisode != null) {
      tvdb_matched = true;

      ResultSet existingRow = getExistingEpisodeRow(tvdbEpisode);
      episode.initializeFromDBObject(existingRow);

      updatedShows++;
    } else {
      episode.initializeForInsert();
      addedShows++;
    }


    updateEpisodeAndSeries(series, tivoEpisode, episode, tvdb_matched);

    return false;
  }

  private void updateEpisodeAndSeries(SeriesPostgres series, TiVoEpisodePostgres tivoEpisode, EpisodePostgres episode, Boolean matched) {
    episode.tivoEpisodeId.changeValue(tivoEpisode.id.getValue());
    episode.tivoProgramId.changeValue(tivoEpisode.programId.getValue());
    episode.onTiVo.changeValue(true);
    episode.seriesId.changeValue(series.id.getValue());

    episode.commit(connection);

    Integer episodeId = tivoEpisode.id.getValue();

    if (episodeId == null) {
      throw new RuntimeException("Episode ID should never be null after insert or update!");
    }

    updateSeriesDenorms(tivoEpisode, episode, series, matched);

    series.commit(connection);
  }

  private TVDBEpisodePostgres retryMatchWithUpdatedTVDB(SeriesPostgres series, Boolean tvdbUpdated, TiVoEpisodePostgres tivoEpisode) throws SQLException {
    if (!tvdbUpdated) {
      TVDBSeriesPostgresUpdater updater = new TVDBSeriesPostgresUpdater(connection, series);
      updater.updateSeries();

      return findTVDBEpisodeMatch(tivoEpisode, series.id.getValue());
    }
    return null;
  }

  private TiVoEpisodePostgres getOrCreateTiVoEpisode(NodeList showDetails, TivoInfo tivoInfo, ResultSet existingEpisode, Boolean tivoEpisodeExists) throws SQLException {
    TiVoEpisodePostgres tivoEpisode = new TiVoEpisodePostgres();
    if (tivoEpisodeExists) {
      tivoEpisode.initializeFromDBObject(existingEpisode);
    } else {
      tivoEpisode.initializeForInsert();
    }

    formatEpisodeObject(tivoEpisode, tivoInfo.url, tivoInfo.isSuggestion, showDetails);
    tivoEpisode.tivoSeriesId.changeValue(tivoInfo.tivoId);
    tivoEpisode.seriesTitle.changeValue(tivoInfo.seriesTitle);
    tivoEpisode.commit(connection);
    return tivoEpisode;
  }

  private void addNewSeries(SeriesPostgres series, TivoInfo tivoInfo) throws SQLException {
    series.initializeForInsert();

    debug("Adding series '" + tivoInfo.seriesTitle + "'  with TiVoID '" + tivoInfo.tivoId + "'");

    Integer tier = tivoInfo.isSuggestion ? 5 : 4;

    series.tivoSeriesId.changeValue(tivoInfo.tivoId);
    series.seriesTitle.changeValue(tivoInfo.seriesTitle);
    series.isSuggestion.changeValue(tivoInfo.isSuggestion);
    series.tier.changeValue(tier);
    series.matchedWrong.changeValue(false);

    series.initializeDenorms();

    series.commit(connection);

    series.addViewingLocation(connection, "TiVo");

    addedShows++;
  }

  private ResultSet getExistingEpisodeRow(TVDBEpisodePostgres tvdbMatch) {
    Integer tvdb_id = tvdbMatch.id.getValue();
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM episode " +
            "WHERE tvdb_episode_id = ?",
        tvdb_id
    );
    if (!connection.hasMoreElements(resultSet)) {
      throw new RuntimeException("No episode row found pointing to existing TVDB_episode with ID " + tvdb_id);
    }
    return resultSet;
  }

  private ResultSet getExistingEpisodeRow(TiVoEpisodePostgres tiVoEpisodePostgres) {
    Integer tivoEpisodeId = tiVoEpisodePostgres.id.getValue();
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM episode " +
            "WHERE tivo_episode_id = ?",
        tivoEpisodeId
    );
    if (!connection.hasMoreElements(resultSet)) {
      throw new RuntimeException("No episode row found pointing to existing tivo_episode with ID " + tivoEpisodeId);
    }
    return resultSet;
  }


  private Boolean isAfter(Date trackingDate, Date newDate) {
    return trackingDate == null || trackingDate.before(newDate);
  }


  private void updateSeriesDenorms(TiVoEpisodePostgres tiVoEpisode, EpisodePostgres episode, SeriesPostgres series, Boolean matched) {

    Boolean suggestion = tiVoEpisode.suggestion.getValue();
    Date showingStartTime = tiVoEpisode.showingStartTime.getValue();
    Boolean watched = episode.watched.getValue();

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

  private ResultSet getExistingTiVoEpisodes(String programId) {
    return connection.prepareAndExecuteStatementFetch("SELECT * FROM tivo_episode WHERE program_id = ?", programId);
  }

  private TiVoEpisodePostgres formatEpisodeObject(TiVoEpisodePostgres episode, String url, Boolean isSuggestion, NodeList showDetails) {
    episode.captureDate.changeValueFromXMLString(getValueOfSimpleStringNode(showDetails, "CaptureDate"));
    episode.showingStartTime.changeValueFromXMLString(getValueOfSimpleStringNode(showDetails, "ShowingStartTime"));

    episode.description.changeValueFromString(getValueOfSimpleStringNode(showDetails, "Description"));
    episode.title.changeValueFromString(getValueOfSimpleStringNode(showDetails, "EpisodeTitle"));
    episode.episodeNumber.changeValueFromString(getValueOfSimpleStringNode(showDetails, "EpisodeNumber"));
    episode.hd.changeValueFromString(getValueOfSimpleStringNode(showDetails, "HighDefinition"));
    episode.programId.changeValueFromString(getValueOfSimpleStringNode(showDetails, "ProgramId"));
    episode.duration.changeValueFromString(getValueOfSimpleStringNode(showDetails, "Duration"));
    episode.showingDuration.changeValueFromString(getValueOfSimpleStringNode(showDetails, "ShowingDuration"));
    episode.channel.changeValueFromString(getValueOfSimpleStringNode(showDetails, "SourceChannel"));
    episode.station.changeValueFromString(getValueOfSimpleStringNode(showDetails, "SourceStation"));
    episode.rating.changeValueFromString(getValueOfSimpleStringNode(showDetails, "TvRating"));
    episode.retired.changeValue(0);

    episode.suggestion.changeValue(isSuggestion);
    episode.url.changeValue(url);

    return episode;
  }

  private TVDBEpisodePostgres findTVDBEpisodeMatch(TiVoEpisodePostgres tivoEpisode, Integer seriesId) throws SQLException {
    String episodeTitle = tivoEpisode.title.getValue();
    Integer episodeNumber = tivoEpisode.episodeNumber.getValue();
    Date startTime = tivoEpisode.showingStartTime.getValue();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT te.* " +
            "FROM episode e " +
            "INNER JOIN tvdb_episode te " +
            "  ON e.tvdb_episode_id = te.id " +
            "WHERE e.tivo_episode_id IS NULL " +
            "AND e.seriesid = ?",
        seriesId
    );

    List<TVDBEpisodePostgres> tvdbEpisodes = new ArrayList<>();

    while(connection.hasMoreElements(resultSet)) {
      TVDBEpisodePostgres tvdbEpisode = new TVDBEpisodePostgres();
      tvdbEpisode.initializeFromDBObject(resultSet);
      tvdbEpisodes.add(tvdbEpisode);
    }

    if (episodeTitle != null) {
      for (TVDBEpisodePostgres tvdbEpisode : tvdbEpisodes) {
        String tvdbTitleObject = tvdbEpisode.name.getValue();

        if (episodeTitle.equalsIgnoreCase(tvdbTitleObject)) {
          return tvdbEpisode;
        }
      }
    }

    // no match found on episode title. Try episode number.

    if (episodeNumber != null) {
      Integer seasonNumber = 1;

      if (episodeNumber < 100) {
        TVDBEpisodePostgres match = checkForNumberMatch(seasonNumber, episodeNumber, tvdbEpisodes);
        if (match != null) {
          return match;
        }
      } else {
        String episodeNumberString = episodeNumber.toString();
        int seasonLength = episodeNumberString.length() / 2;

        String seasonString = episodeNumberString.substring(0, seasonLength);
        String episodeString = episodeNumberString.substring(seasonLength, episodeNumberString.length());

        TVDBEpisodePostgres match = checkForNumberMatch(Integer.valueOf(seasonString), Integer.valueOf(episodeString), tvdbEpisodes);

        if (match != null) {
          return match;
        }
      }
    }

    // no match on episode number. Try air date.

    if (startTime != null) {
      DateTime showingStartTime = new DateTime(startTime);

      for (TVDBEpisodePostgres tvdbEpisode : tvdbEpisodes) {
        Date firstAiredValue = tvdbEpisode.firstAired.getValue();
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


  private TVDBEpisodePostgres checkForNumberMatch(Integer seasonNumber, Integer episodeNumber, List<TVDBEpisodePostgres> tvdbEpisodes) {
    for (TVDBEpisodePostgres tvdbEpisode : tvdbEpisodes) {
      Integer tvdbSeason = tvdbEpisode.seasonNumber.getValue();
      Integer tvdbEpisodeNumber = tvdbEpisode.episodeNumber.getValue();

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
    Node customIcon = getNullableNodeWithTag(links, "CustomIcon");
    if (customIcon == null) {
      return false;
    } else {
      NodeList customIcons = customIcon.getChildNodes();
      String iconUrl = getValueOfSimpleStringNode(customIcons, "Url");

      return iconUrl != null && iconUrl.endsWith("suggestion-recording");
    }
  }

  @NotNull
  private Boolean isEpisodic(NodeList showAttributes) {
    String detailUrl = getDetailUrl(showAttributes);

    try {
      Document document = readXMLFromTivoUrl(detailUrl);

      debug("Checking against DB...");
      return parseDetailFromDocument(document);
    } catch (SAXException | IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Error reading from URL: " + detailUrl);
    } catch (EpisodeAlreadyFoundException e) {
      e.printStackTrace();
      throw new RuntimeException("Episode already found.");
    }
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
    Node customIcon = getNullableNodeWithTag(links, "CustomIcon");
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

  @NotNull
  private Node getNodeWithTag(NodeList nodeList, String tag) {
    for (int x = 0; x < nodeList.getLength(); x++) {
      Node item = nodeList.item(x);
      if (tag.equals(item.getNodeName())) {
        return item;
      }
    }
    throw new RuntimeException("No node found with tag '" + tag + "'");
  }

  @Nullable
  private Node getNullableNodeWithTag(NodeList nodeList, String tag) {
    for (int x = 0; x < nodeList.getLength(); x++) {
      Node item = nodeList.item(x);
      if (tag.equals(item.getNodeName())) {
        return item;
      }
    }
    return null;
  }

  private String getValueOfSimpleStringNode(NodeList nodeList, String tag) {
    Node nodeWithTag = getNullableNodeWithTag(nodeList, tag);
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

  public Document readXMLFromTivoUrl(String urlString) throws IOException, SAXException {

    Authenticator.setDefault (new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("tivo", "4649000153".toCharArray());
      }
    });

    URL url = new URL(urlString);

    HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
    try (InputStream is = conn.getInputStream()) {
      return recoverDocument(is);
    }
  }


  protected Document recoverDocument(InputStream inputStream) throws IOException, SAXException {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = null;
    try {
      dBuilder = dbFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }

    Document doc;
    assert dBuilder != null;
    doc = dBuilder.parse(inputStream);
    return doc;
  }



  protected void debug(Object object) {
    System.out.println(object);
  }


  private class EpisodeAlreadyFoundException extends Exception {
    public EpisodeAlreadyFoundException(String message) {
      super(message);
    }
  }

}

