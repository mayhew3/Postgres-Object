package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.SSLTool;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.*;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.NodeReader;
import com.mayhew3.gamesutil.xml.NodeReaderImpl;
import com.sun.istack.internal.NotNull;
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
import java.net.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TiVoCommunicator {

  private Boolean lookAtAllShows = false;
  private List<String> episodesOnTiVo;
  private List<String> moviesOnTiVo;
  private NodeReader nodeReader;

  private Integer addedShows = 0;
  private Integer deletedShows = 0;
  private Integer updatedShows = 0;

  private static SQLConnection sqlConnection;

  public TiVoCommunicator(SQLConnection connection) {
    episodesOnTiVo = new ArrayList<>();
    moviesOnTiVo = new ArrayList<>();
    nodeReader = new NodeReaderImpl();
    sqlConnection = connection;
  }

  public static void main(String[] args) throws UnknownHostException, SQLException, URISyntaxException, BadlyFormattedXMLException {
    List<String> argList = Lists.newArrayList(args);
    Boolean lookAtAllShows = argList.contains("FullMode");
    Boolean dev = argList.contains("Dev");

    String identifier = new ArgumentChecker(args).getDBIdentifier();

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);

    TiVoCommunicator tiVoCommunicator = new TiVoCommunicator(connection);

    if (dev) {
      tiVoCommunicator.truncateTables();
    }

    tiVoCommunicator.runUpdate(lookAtAllShows);

    new SeriesDenormUpdater(connection).updateFields();
  }

  public void runUpdate(Boolean updateAllShows) throws SQLException, BadlyFormattedXMLException {

    lookAtAllShows = updateAllShows;

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

  }

  private void truncateTables() throws SQLException {
    sqlConnection.executeUpdate("TRUNCATE TABLE tvdb_series CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE tvdb_episode CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE tivo_episode CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE genre CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE viewing_location CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE edge_tivo_episode CASCADE");

    sqlConnection.executeUpdate("ALTER SEQUENCE series_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE tvdb_series_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE season_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE episode_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE tivo_episode_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE tvdb_episode_id_seq RESTART WITH 1");

    sqlConnection.executeUpdate("ALTER SEQUENCE genre_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE series_genre_id_seq RESTART WITH 1");

    sqlConnection.executeUpdate("ALTER SEQUENCE viewing_location_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE series_viewing_location_id_seq RESTART WITH 1");

  }


  public void updateFields() throws SQLException, BadlyFormattedXMLException {
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
        checkForDeletedMovies();
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
    ResultSet resultSet = sqlConnection.executeQuery(
        "SELECT * " +
            "FROM tivo_episode " +
            "WHERE deleted_date IS NULL"
    );

    while (resultSet.next()) {
      TiVoEpisode tiVoEpisode = new TiVoEpisode();
      tiVoEpisode.initializeFromDBObject(resultSet);

      deleteIfGone(tiVoEpisode);
    }

  }

  private void checkForDeletedMovies() throws SQLException {
    ResultSet resultSet = sqlConnection.executeQuery(
        "SELECT * " +
            "FROM movie " +
            "WHERE deleted_date IS NULL"
    );

    while (resultSet.next()) {
      Movie movie = new Movie();
      movie.initializeFromDBObject(resultSet);

      deleteIfGone(movie);
    }

  }

  private void deleteIfGone(TiVoEpisode episode) throws SQLException {
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
      episode.commit(sqlConnection);

      deletedShows++;
    }
  }

  private void deleteIfGone(Movie movie) throws SQLException {
    String programId = movie.programId.getValue();

    if (programId == null) {
      throw new RuntimeException("TiVo Movie found with TiVoProgramId 'null'.");
    }

    if (!moviesOnTiVo.contains(programId)) {
      Date showingTime = movie.showingStartTime.getValue();

      if (showingTime == null) {
        debug("Found movie in DB that is no longer on Tivo: '" + movie.title.getValue() + "' on UNKNOWN DATE. Updating deletion date.");
      } else {
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(showingTime);
        debug("Found movie in DB that is no longer on Tivo: '" + movie.title.getValue() + "' on " + formattedDate + ". Updating deletion date.");
      }

      movie.deletedDate.changeValue(new Date());
      movie.commit(sqlConnection);

      deletedShows++;
    }
  }

  private boolean parseShowsFromDocument(Document document) throws SQLException, BadlyFormattedXMLException {
    NodeList nodeList = document.getChildNodes();

    Node tivoContainer = nodeReader.getNodeWithTag(nodeList, "TiVoContainer");
    NodeList childNodes = tivoContainer.getChildNodes();

    Boolean shouldBeLastDocument = false;

    Integer showNumber = 0;
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node item = childNodes.item(i);
      if (item.getNodeName().equals("Item")) {
        NodeList showAttributes = item.getChildNodes();
        Boolean alreadyExists = false;

        try {
          alreadyExists = parseAndUpdateSingleShow(showAttributes);
        } catch (Exception e) {
          e.printStackTrace();
          debug("Failed to parse TVDB data.");
        }

        if (alreadyExists) {
          shouldBeLastDocument = true;
        }
        showNumber++;
      }
    }

    return !shouldBeLastDocument && showNumber == 50;
  }


  /**
   * @param showAttributes Root XML object
   * @return Whether this episode already exists in the database, and we should stop updating later episodes in quick mode.
   * @throws SQLException
   */
  private Boolean parseAndUpdateSingleShow(NodeList showAttributes) throws SQLException, BadlyFormattedXMLException, ShowFailedException {

    if (isRecordingNow(showAttributes)) {
      debug("Skipping episode that is currently recording.");
      return false;
    }

    NodeList showDetails = nodeReader.getNodeWithTag(showAttributes, "Details").getChildNodes();

    TiVoInfo tivoInfo = new TiVoInfo(showDetails, nodeReader);
    tivoInfo.isSuggestion = isSuggestion(showAttributes);
    tivoInfo.url = getUrl(showAttributes);

    if (isEpisodic(showAttributes)) {

      Series series = getOrCreateSeries(tivoInfo);
      return addEpisodeIfNotExists(showDetails, series, tivoInfo);
    } else {
      return addMovieIfNotExists(showDetails, tivoInfo);
    }
  }

  /**
   * @return New series object based on TiVo info, or existing series object in DB.
   * @throws SQLException
   */
  @NotNull
  private Series getOrCreateSeries(TiVoInfo tivoInfo) throws SQLException {
    ResultSet resultSet = sqlConnection.prepareAndExecuteStatementFetch("SELECT * FROM series WHERE tivo_series_ext_id = ?", tivoInfo.tivoId);

    Series series = new Series();

    if (!resultSet.next()) {
      addNewSeries(series, tivoInfo);
    } else {
      series.initializeFromDBObject(resultSet);
      debug("Updating existing series '" + tivoInfo.seriesTitle + "'.");
    }
    return series;
  }

  /**
   * @return Whether the episode already exists in the database.
   * @throws SQLException
   */
  private Boolean addEpisodeIfNotExists(NodeList showDetails, Series series, TiVoInfo tivoInfo) throws SQLException, BadlyFormattedXMLException {
    ResultSet existingTiVoEpisode = getExistingTiVoEpisodes(tivoInfo.programId);
    Boolean tivoEpisodeExists = existingTiVoEpisode.next();

    if (lookAtAllShows) {
      episodesOnTiVo.add(tivoInfo.programId);
    }

    if (tivoEpisodeExists && !lookAtAllShows) {
      return true;
    }

    TiVoEpisode tivoEpisode = getOrCreateTiVoEpisode(showDetails, tivoInfo, existingTiVoEpisode, tivoEpisodeExists);

    List<Episode> linkedEpisodes = tivoEpisode.getEpisodes(sqlConnection);

    // if we already have any linked episodes, don't try to match again.
    if (linkedEpisodes.isEmpty()) {

      TVDBEpisodeMatcher matcher = new TVDBEpisodeMatcher(sqlConnection, tivoEpisode, series.id.getValue());
      TVDBEpisode tvdbEpisode = matcher.findTVDBEpisodeMatch();

      // if we found a good match for tivo episode, link it
      if (tvdbEpisode != null) {
        Episode episode = tvdbEpisode.getEpisode(sqlConnection);

        updatedShows++;

        episode.addToTiVoEpisodes(sqlConnection, tivoEpisode);
        updateSeriesDenorms(tivoEpisode, episode, series);
        series.commit(sqlConnection);
      }
    }

    return false;
  }


  /**
   * @return Whether the movie already exists in the database.
   * @throws SQLException
   */
  private Boolean addMovieIfNotExists(NodeList showDetails, TiVoInfo tivoInfo) throws SQLException {
    ResultSet resultSet = getExistingMovies(tivoInfo.programId);
    boolean movieExists = resultSet.next();

    if (lookAtAllShows) {
      moviesOnTiVo.add(tivoInfo.programId);
    }

    if (movieExists && !lookAtAllShows) {
      return true;
    }

    getOrCreateMovie(showDetails, tivoInfo, resultSet, movieExists);

    return false;
  }

  private TiVoEpisode getOrCreateTiVoEpisode(NodeList showDetails, TiVoInfo tivoInfo, ResultSet existingEpisode, Boolean tivoEpisodeExists) throws SQLException {
    TiVoEpisode tivoEpisode = new TiVoEpisode();
    if (tivoEpisodeExists) {
      tivoEpisode.initializeFromDBObject(existingEpisode);
    } else {
      tivoEpisode.initializeForInsert();
    }

    formatEpisodeObject(tivoEpisode, tivoInfo.url, tivoInfo.isSuggestion, showDetails);
    tivoEpisode.tivoSeriesExtId.changeValue(tivoInfo.tivoId);
    tivoEpisode.seriesTitle.changeValue(tivoInfo.seriesTitle);

    // todo: check for duplicate (program_id, retired). Do some research. In these cases, are two episodes
    // todo: on TiVo with same program_id, or is one deleted but not marked yet? Either way I think we should update
    // todo: instead of insert.
    tivoEpisode.commit(sqlConnection);
    return tivoEpisode;
  }

  private Movie getOrCreateMovie(NodeList showDetails, TiVoInfo tivoInfo, ResultSet existingMovie, Boolean movieExists) throws SQLException {
    Movie movie = new Movie();
    if (movieExists) {
      movie.initializeFromDBObject(existingMovie);
    } else {
      movie.initializeForInsert();
    }

    formatMovieObject(movie, tivoInfo.url, tivoInfo.isSuggestion, showDetails);
    movie.tivoSeriesExtId.changeValue(tivoInfo.tivoId);
    movie.seriesTitle.changeValue(tivoInfo.seriesTitle);

    // todo: check for duplicate (program_id, retired). Do some research. In these cases, are two episodes
    // todo: on TiVo with same program_id, or is one deleted but not marked yet? Either way I think we should update
    // todo: instead of insert.
    movie.commit(sqlConnection);
    return movie;
  }

  private void addNewSeries(Series series, TiVoInfo tivoInfo) throws SQLException {
    series.initializeForInsert();

    debug("Adding series '" + tivoInfo.seriesTitle + "'  with TiVoID '" + tivoInfo.tivoId + "'");

    Integer tier = tivoInfo.isSuggestion ? 5 : 4;

    series.tivoSeriesExtId.changeValue(tivoInfo.tivoId);
    series.seriesTitle.changeValue(tivoInfo.seriesTitle);
    series.tivoName.changeValue(tivoInfo.seriesTitle);
    series.isSuggestion.changeValue(tivoInfo.isSuggestion);
    series.tier.changeValue(tier);
    series.matchedWrong.changeValue(false);

    series.initializeDenorms();

    series.commit(sqlConnection);

    series.addViewingLocation(sqlConnection, "TiVo");

    addedShows++;
  }


  private Boolean isAfter(Date trackingDate, Date newDate) {
    return trackingDate == null || trackingDate.before(newDate);
  }


  private void updateSeriesDenorms(TiVoEpisode tiVoEpisode, Episode episode, Series series) {

    Boolean suggestion = tiVoEpisode.suggestion.getValue();
    Date showingStartTime = tiVoEpisode.showingStartTime.getValue();
    Boolean watched = episode.watched.getValue();

    if (suggestion) {
      series.suggestionEpisodes.increment(1);
    } else {
      series.activeEpisodes.increment(1);
      series.unwatchedEpisodes.increment(1);
    }

    series.matchedEpisodes.increment(1);
    series.tvdbOnlyEpisodes.increment(-1);
    if (!watched) {
      series.unwatchedUnrecorded.increment(-1);
    }

    if (isAfter(series.mostRecent.getValue(), showingStartTime)) {
      series.mostRecent.changeValue(showingStartTime);
    }

    if (isAfter(series.lastUnwatched.getValue(), showingStartTime)) {
      series.lastUnwatched.changeValue(showingStartTime);
    }

  }

  private ResultSet getExistingTiVoEpisodes(String programId) throws SQLException {
    return sqlConnection.prepareAndExecuteStatementFetch("SELECT * FROM tivo_episode WHERE program_id = ? AND retired = ?", programId, 0);
  }

  private ResultSet getExistingMovies(String programId) throws SQLException {
    return sqlConnection.prepareAndExecuteStatementFetch("SELECT * FROM movie WHERE program_id = ? AND retired = ?", programId, 0);
  }

  private TiVoEpisode formatEpisodeObject(TiVoEpisode episode, String url, Boolean isSuggestion, NodeList showDetails) {
    episode.captureDate.changeValueFromXMLString(nodeReader.getValueOfSimpleStringNode(showDetails, "CaptureDate"));
    episode.showingStartTime.changeValueFromXMLString(nodeReader.getValueOfSimpleStringNode(showDetails, "ShowingStartTime"));

    episode.description.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "Description"));
    episode.title.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "EpisodeTitle"));
    episode.episodeNumber.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "EpisodeNumber"));
    episode.hd.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "HighDefinition"));
    episode.programId.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "ProgramId"));
    episode.duration.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "Duration"));
    episode.showingDuration.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "ShowingDuration"));
    episode.channel.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "SourceChannel"));
    episode.station.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "SourceStation"));
    episode.rating.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "TvRating"));
    episode.retired.changeValue(0);

    episode.suggestion.changeValue(isSuggestion);
    episode.url.changeValue(url);

    return episode;
  }

  private Movie formatMovieObject(Movie movie, String url, Boolean isSuggestion, NodeList showDetails) {
    movie.captureDate.changeValueFromXMLString(nodeReader.getValueOfSimpleStringNode(showDetails, "CaptureDate"));
    movie.showingStartTime.changeValueFromXMLString(nodeReader.getValueOfSimpleStringNode(showDetails, "ShowingStartTime"));

    movie.description.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "Description"));
    movie.title.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "Title"));
    movie.hd.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "HighDefinition"));
    movie.programId.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "ProgramId"));
    movie.duration.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "Duration"));
    movie.showingDuration.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "ShowingDuration"));
    movie.channel.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "SourceChannel"));
    movie.station.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "SourceStation"));
    movie.rating.changeValueFromString(nodeReader.getValueOfSimpleStringNode(showDetails, "TvRating"));
    movie.retired.changeValue(0);

    movie.suggestion.changeValue(isSuggestion);
    movie.url.changeValue(url);

    return movie;
  }


  private Boolean isSuggestion(NodeList showAttributes) throws BadlyFormattedXMLException {
    NodeList links = nodeReader.getNodeWithTag(showAttributes, "Links").getChildNodes();
    Node customIcon = nodeReader.getNullableNodeWithTag(links, "CustomIcon");
    if (customIcon == null) {
      return false;
    } else {
      NodeList customIcons = customIcon.getChildNodes();
      String iconUrl = nodeReader.getValueOfSimpleStringNode(customIcons, "Url");

      return iconUrl != null && iconUrl.endsWith("suggestion-recording");
    }
  }

  @NotNull
  private Boolean isEpisodic(NodeList showAttributes) throws BadlyFormattedXMLException {
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
    } catch (BadlyFormattedXMLException e) {
      e.printStackTrace();
      throw new RuntimeException("XML formatting error!");
    }
  }


  private boolean parseDetailFromDocument(Document document) throws EpisodeAlreadyFoundException, BadlyFormattedXMLException {
    NodeList nodeList = document.getChildNodes();

    NodeList tvBus = nodeReader.getNodeWithTag(nodeList, "TvBusMarshalledStruct:TvBusEnvelope").getChildNodes();
    NodeList showing = nodeReader.getNodeWithTag(tvBus, "showing").getChildNodes();
    NodeList program = nodeReader.getNodeWithTag(showing, "program").getChildNodes();
    NodeList series = nodeReader.getNodeWithTag(program, "series").getChildNodes();

    String isEpisodic = nodeReader.getValueOfSimpleStringNode(series, "isEpisodic");
    return Boolean.parseBoolean(isEpisodic);
  }


  private Boolean isRecordingNow(NodeList showAttributes) throws BadlyFormattedXMLException {
    NodeList links = nodeReader.getNodeWithTag(showAttributes, "Links").getChildNodes();
    Node customIcon = nodeReader.getNullableNodeWithTag(links, "CustomIcon");
    if (customIcon == null) {
      return false;
    } else {
      NodeList customIcons = customIcon.getChildNodes();
      String iconUrl = nodeReader.getValueOfSimpleStringNode(customIcons, "Url");

      return iconUrl != null && iconUrl.endsWith("in-progress-recording");
    }
  }

  private String getUrl(NodeList showAttributes) throws BadlyFormattedXMLException {
    NodeList links = nodeReader.getNodeWithTag(showAttributes, "Links").getChildNodes();
    NodeList content = nodeReader.getNodeWithTag(links, "Content").getChildNodes();
    return nodeReader.getValueOfSimpleStringNode(content, "Url");
  }

  private String getDetailUrl(NodeList showAttributes) throws BadlyFormattedXMLException {
    NodeList links = nodeReader.getNodeWithTag(showAttributes, "Links").getChildNodes();
    NodeList content = nodeReader.getNodeWithTag(links, "TiVoVideoDetails").getChildNodes();
    return nodeReader.getValueOfSimpleStringNode(content, "Url");
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

