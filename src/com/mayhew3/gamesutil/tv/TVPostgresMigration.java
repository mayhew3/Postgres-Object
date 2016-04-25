package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.dataobject.mongo.ConnectLogMongo;
import com.mayhew3.gamesutil.dataobject.mongo.EpisodeMongo;
import com.mayhew3.gamesutil.dataobject.mongo.ErrorLogMongo;
import com.mayhew3.gamesutil.dataobject.mongo.SeriesMongo;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.games.MongoConnection;
import com.mayhew3.gamesutil.dataobject.*;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.sun.istack.internal.Nullable;
import org.bson.types.ObjectId;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TVPostgresMigration {
  private MongoConnection mongoConnection;
  private SQLConnection sqlConnection;

  private static Boolean devMode = false;

  private Map<ObjectId, ShowFailedException> failedEpisodes;
  private Map<String, ShowFailedException> failedMovies;

  public TVPostgresMigration(MongoConnection mongoConnection, SQLConnection sqlConnection) {
    this.failedEpisodes = new HashMap<>();
    this.failedMovies = new HashMap<>();
    this.mongoConnection = mongoConnection;
    this.sqlConnection = sqlConnection;
  }

  public static void main(String[] args) throws SQLException, URISyntaxException {
    List<String> argList = Lists.newArrayList(args);
    devMode = argList.contains("dev");

    SQLConnection sqlConnection = new PostgresConnectionFactory().createLocalConnection();
    TVPostgresMigration tvPostgresMigration = new TVPostgresMigration(
        new MongoConnection("tv"),
        sqlConnection);

    tvPostgresMigration.updatePostgresDatabase();

    new SeriesDenormUpdater(sqlConnection).updateFields();
  }

  public void updatePostgresDatabase() throws SQLException {
    if (devMode) {
      truncatePostgresTables();
    }

    updateAllMovies();
    updateConnectLogs();
    updateErrorLogs();
    updateAllSeries();

    printFailedEpisodes();
    printFailedMovies();
  }

  private void truncatePostgresTables() throws SQLException {
    sqlConnection.executeUpdate("TRUNCATE TABLE tvdb_series CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE tvdb_episode CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE tivo_episode CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE genre CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE viewing_location CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE edge_tivo_episode CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE error_log CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE connect_log CASCADE");
    sqlConnection.executeUpdate("TRUNCATE TABLE movie CASCADE");

    sqlConnection.executeUpdate("ALTER SEQUENCE series_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE tvdb_series_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE season_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE episode_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE tivo_episode_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE tvdb_episode_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE error_log_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE connect_logs_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE movie_id_seq RESTART WITH 1");

    sqlConnection.executeUpdate("ALTER SEQUENCE genre_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE series_genre_id_seq RESTART WITH 1");

    sqlConnection.executeUpdate("ALTER SEQUENCE viewing_location_id_seq RESTART WITH 1");
    sqlConnection.executeUpdate("ALTER SEQUENCE series_viewing_location_id_seq RESTART WITH 1");

  }

  private void updateAllSeries() throws SQLException {
    DBObject dbObject = new BasicDBObject()
        .append("IsEpisodic", true)
        ;

    DBCursor dbCursor = mongoConnection.getCollection("series").find(dbObject);

    int totalRows = dbCursor.count();
    debug(totalRows + " series found to copy. Starting.");

    int i = 0;

    while (dbCursor.hasNext()) {
      i++;
      DBObject seriesMongoObject = dbCursor.next();

      updateSingleSeries(seriesMongoObject);

      debug(i + " out of " + totalRows + " processed.");
      debug("");
    }

    debug("Update complete!");
  }

  private void printFailedEpisodes() {
    Set<ObjectId> failedIds = failedEpisodes.keySet();
    if (!failedIds.isEmpty()) {
      debug(failedIds.size() + " episodes failed!");
      for (ObjectId failedId : failedIds) {
        //noinspection ThrowableResultOfMethodCallIgnored
        debug(" - " + failedId + ": " + failedEpisodes.get(failedId).getLocalizedMessage());
      }
    }
  }

  private void printFailedMovies() {
    Set<String> failedTitles = failedMovies.keySet();
    if (!failedTitles.isEmpty()) {
      debug(failedTitles.size() + " movies failed!");
      for (String title : failedTitles) {
        //noinspection ThrowableResultOfMethodCallIgnored
        debug(" - " + title + ": " + failedMovies.get(title).getLocalizedMessage());
      }
    }
  }


  private void updateAllMovies() throws SQLException {
    DBObject dbObject = new BasicDBObject()
        .append("IsEpisodic", new BasicDBObject("$ne", true))
        ;

    DBCursor dbCursor = mongoConnection.getCollection("series").find(dbObject);

    int totalRows = dbCursor.count();
    debug(totalRows + " movies found to copy. Starting.");

    int i = 0;

    while (dbCursor.hasNext()) {
      i++;
      DBObject movieMongoObject = dbCursor.next();

      SeriesMongo seriesMongo = new SeriesMongo();
      seriesMongo.initializeFromDBObject(movieMongoObject);

      try {
        processSingleMovie(seriesMongo);
      } catch (ShowFailedException e) {
        debug("Movie failed: " + seriesMongo.seriesTitle.getValue());
        //noinspection ThrowableResultOfMethodCallIgnored
        failedMovies.put(seriesMongo.seriesTitle.getValue(), e);
        e.printStackTrace();
      }

      debug(i + " out of " + totalRows + " processed.");
      debug("");
    }



    debug("Update complete!");

  }

  private void processSingleMovie(SeriesMongo seriesMongo) throws ShowFailedException {
    Movie movie = new Movie();
    movie.initializeForInsert();

    movie.onTiVo.changeValue(true);
    copySeriesFieldsToMovie(seriesMongo, movie);

    ObjectId mongoId = seriesMongo._id.getValue();

    BasicDBObject episodeQuery = new BasicDBObject()
        .append("SeriesId", mongoId)
        .append("OnTiVo", true)
        ;

    DBCursor cursor = mongoConnection.getCollection("episodes").find(episodeQuery);

    int numberOfEpisodes = cursor.count();
    if (numberOfEpisodes > 1) {
      throw new ShowFailedException("Movies should have no more than one episode. Found: " + numberOfEpisodes);
    }

    if (cursor.hasNext()) {
      DBObject episodeDBObj = cursor.next();

      EpisodeMongo episodeMongo = new EpisodeMongo();
      episodeMongo.initializeFromDBObject(episodeDBObj);

      copyEpisodeFieldsToMovie(episodeMongo, movie);

      try {
        movie.commit(sqlConnection);
      } catch (SQLException e) {
        e.printStackTrace();
        throw new ShowFailedException("Insert failed for movie: " + movie.seriesTitle.getValue());
      }
    }
  }

  private void copySeriesFieldsToMovie(SeriesMongo seriesMongo, Movie movie) {
    movie.title.changeValue(seriesMongo.seriesTitle.getValue());
    movie.tier.changeValue(seriesMongo.tier.getValue());
    movie.metacritic.changeValue(seriesMongo.metacritic.getValue());
    movie.metacriticHint.changeValue(seriesMongo.metacriticHint.getValue());
    movie.my_rating.changeValue(seriesMongo.myRating.getValue());
    movie.tivoName.changeValue(seriesMongo.tivoName.getValue());
  }

  private void copyEpisodeFieldsToMovie(EpisodeMongo episodeMongo, Movie movie) {
    movie.isSuggestion.changeValue(episodeMongo.tivoSuggestion.getValue());
    movie.showingStartTime.changeValue(episodeMongo.tivoShowingStartTime.getValue());
    movie.deletedDate.changeValue(episodeMongo.tivoDeletedDate.getValue());
    movie.captureDate.changeValue(episodeMongo.tivoCaptureDate.getValue());
    movie.hd.changeValue(episodeMongo.tivoHD.getValue());
    movie.duration.changeValue(episodeMongo.tivoDuration.getValue());
    movie.showingDuration.changeValue(episodeMongo.tivoShowingDuration.getValue());
    movie.channel.changeValue(episodeMongo.tivoChannel.getValue());
    movie.rating.changeValue(episodeMongo.tivoRating.getValue());
    movie.tivoSeriesId.changeValue(episodeMongo.tivoSeriesId.getValue());
    movie.programId.changeValue(episodeMongo.tivoProgramId.getValue());
    movie.seriesTitle.changeValue(episodeMongo.tivoSeriesTitle.getValue());
    movie.description.changeValue(episodeMongo.tivoDescription.getValue());
    movie.station.changeValue(episodeMongo.tivoStation.getValue());
    movie.url.changeValue(episodeMongo.tivoUrl.getValue());
    movie.watched.changeValue(episodeMongo.watched.getValue());
    movie.watchedDate.changeValue(episodeMongo.watchedDate.getValue());
    movie.dateAdded.changeValue(episodeMongo.dateAdded.getValue());
  }

  private void updateErrorLogs() throws SQLException {
    DBCursor dbCursor = mongoConnection.getCollection("errorlogs").find();

    int totalRows = dbCursor.count();
    debug(totalRows + " errorlogs found to copy. Starting.");

    int i = 0;

    while (dbCursor.hasNext()) {
      i++;
      DBObject errorLogMongoObject = dbCursor.next();

      ErrorLogMongo errorLogMongo = new ErrorLogMongo();
      errorLogMongo.initializeFromDBObject(errorLogMongoObject);

      ErrorLog errorLog = new ErrorLog();
      errorLog.initializeForInsert();

      copyAllErrorLogFields(errorLogMongo, errorLog);

      errorLog.commit(sqlConnection);

      debug(i + " out of " + totalRows + " processed.");
      debug("");
    }

    debug("Update complete!");

  }

  private void updateConnectLogs() throws SQLException {
    DBCursor dbCursor = mongoConnection.getCollection("connectlogs").find();

    int totalRows = dbCursor.count();
    debug(totalRows + " connectlogs found to copy. Starting.");

    int i = 0;

    while (dbCursor.hasNext()) {
      i++;
      DBObject connectLogMongoObject = dbCursor.next();

      ConnectLogMongo connectLogMongo = new ConnectLogMongo();
      connectLogMongo.initializeFromDBObject(connectLogMongoObject);

      ConnectLog connectLog = new ConnectLog();
      connectLog.initializeForInsert();

      copyAllConnectLogFields(connectLogMongo, connectLog);

      connectLog.commit(sqlConnection);

      debug(i + " out of " + totalRows + " processed.");
      debug("");
    }

    debug("Update complete!");

  }

  private void copyAllErrorLogFields(ErrorLogMongo errorLogMongo, ErrorLog errorLog) {
    errorLog.chosenName.changeValue(errorLogMongo.chosenName.getValue());
    errorLog.errorMessage.changeValue(errorLogMongo.errorMessage.getValue());
    errorLog.errorType.changeValue(errorLogMongo.errorType.getValue());
    errorLog.eventDate.changeValue(errorLogMongo.eventDate.getValue());
    errorLog.formattedName.changeValue(errorLogMongo.formattedName.getValue());
    errorLog.resolved.changeValue(errorLogMongo.resolved.getValue());
    errorLog.resolvedDate.changeValue(errorLogMongo.resolvedDate.getValue());
    errorLog.tvdbName.changeValue(errorLogMongo.tvdbName.getValue());
    errorLog.tivoId.changeValue(errorLogMongo.tivoId.getValue());
    errorLog.tivoName.changeValue(errorLogMongo.tivoName.getValue());
    errorLog.context.changeValue(errorLogMongo.context.getValue());
    errorLog.ignoreError.changeValue(errorLogMongo.ignoreError.getValue());
  }


  private void copyAllConnectLogFields(ConnectLogMongo connectLogMongo, ConnectLog connectLog) {
    connectLog.startTime.changeValue(connectLogMongo.startTime.getValue());
    connectLog.endTime.changeValue(connectLogMongo.endTime.getValue());
    connectLog.addedShows.changeValue(connectLogMongo.addedShows.getValue());
    connectLog.connectionID.changeValue(connectLogMongo.connectionID.getValue());
    connectLog.deletedShows.changeValue(connectLogMongo.deletedShows.getValue());
    connectLog.tvdbEpisodesAdded.changeValue(connectLogMongo.tvdbEpisodesAdded.getValue());
    connectLog.tvdbEpisodesUpdated.changeValue(connectLogMongo.tvdbEpisodesUpdated.getValue());
    connectLog.tvdbSeriesUpdated.changeValue(connectLogMongo.tvdbSeriesUpdated.getValue());
    connectLog.timeConnected.changeValue(connectLogMongo.timeConnected.getValue());
    connectLog.updatedShows.changeValue(connectLogMongo.updatedShows.getValue());
    connectLog.fastUpdate.changeValue(connectLogMongo.fastUpdate.getValue());
  }


  private void updateSingleSeries(DBObject seriesMongoObject) throws SQLException {
    SeriesMongo seriesMongo = new SeriesMongo();
    seriesMongo.initializeFromDBObject(seriesMongoObject);

    Series series = getOrCreateSeriesPostgres(seriesMongo);

    String title = seriesMongo.seriesTitle.getValue();

    if (series.id.getValue() != null) {
      debug(title + ": Updating");
    } else {
      debug(title + ": Inserting");
    }

    Integer tvdbSeriesId = updateTVDBSeries(seriesMongo);

    series.tvdbSeriesId.changeValue(tvdbSeriesId);
    copyAllSeriesFields(seriesMongo, series);
    series.commit(sqlConnection);

    Integer seriesId = series.id.getValue();
    if (seriesId == null) {
      throw new RuntimeException("No ID populated on series postgres object after insert or update.");
    }

    updateGenres(seriesMongo, series);
    updateViewingLocations(seriesMongo, series);
    updateMetacriticSeasons(seriesMongo, series);
    updatePossibleMatches(seriesMongo, series);
    updateEpisodes(seriesMongo, series);
  }


  private Integer updateTVDBSeries(SeriesMongo seriesMongo) throws SQLException {
    if (seriesMongo.tvdbId.getValue() == null) {
      return null;
    }

    debug("   (also copying tvdb info...)");

    TVDBSeries tvdbSeries = getOrCreateTVDBSeriesPostgres(seriesMongo.tvdbId.getValue());

    copyAllTVDBSeriesFields(seriesMongo, tvdbSeries);
    tvdbSeries.commit(sqlConnection);

    Integer tvdbSeriesId = tvdbSeries.id.getValue();

    if (tvdbSeriesId == null) {
      throw new RuntimeException("No ID populated on tvdb_series postgres object after insert or update.");
    }
    return tvdbSeriesId;
  }

  private void updateMetacriticSeasons(SeriesMongo seriesMongo, Series series) throws SQLException {
    BasicDBList metacriticSeasons = seriesMongo.metacriticSeasons.getValue();
    if (metacriticSeasons != null) {
      for (Object obj : metacriticSeasons) {
        DBObject dbObject = (DBObject) obj;
        Integer seasonNumber = (Integer) dbObject.get("SeasonNumber");
        Integer seasonMetacritic = (Integer) dbObject.get("SeasonMetacritic");

        debug(" - Add season " + seasonNumber + " with Metacritic " + seasonMetacritic);

        Season season = series.getOrCreateSeason(sqlConnection, seasonNumber);
        season.metacritic.changeValue(seasonMetacritic);
        season.commit(sqlConnection);
      }
    }
  }

  private void updateViewingLocations(SeriesMongo seriesMongo, Series series) throws SQLException {
    BasicDBList viewingLocations = seriesMongo.viewingLocations.getValue();
    if (viewingLocations != null) {
      for (Object obj : viewingLocations) {
        String viewingLocation = (String) obj;
        debug(" - Add viewing location '" + viewingLocation + "'");
        series.addViewingLocation(sqlConnection, viewingLocation);
      }
    }
  }

  private void updateGenres(SeriesMongo seriesMongo, Series series) throws SQLException {
    BasicDBList tvdbGenre = seriesMongo.tvdbGenre.getValue();
    if (tvdbGenre != null) {
      for (Object obj : tvdbGenre) {
        String genreName = (String) obj;
        debug(" - Add genre '" + genreName + "'");
        series.addGenre(sqlConnection, genreName);
      }
    }
  }

  private void updatePossibleMatches(SeriesMongo seriesMongo, Series series) throws SQLException {
    BasicDBList dbList = seriesMongo.possibleMatches.getValue();
    if (dbList != null) {
      for (Object obj : dbList) {
        DBObject possibleMatch = (DBObject) obj;
        Integer seriesID = (Integer) possibleMatch.get("SeriesID");
        String title = (String) possibleMatch.get("SeriesTitle");
        series.addPossibleSeriesMatch(sqlConnection, seriesID, title);
      }
    }
  }

  private Series getOrCreateSeriesPostgres(SeriesMongo seriesMongo) throws SQLException {
    String tivoSeriesId = seriesMongo.tivoSeriesId.getValue();
    Integer tvdbId = seriesMongo.tvdbId.getValue();
    if (tivoSeriesId != null) {
      return getOrCreateSeriesPostgresFromTiVoID(tivoSeriesId);
    } else if (tvdbId != null) {
      return getOrCreateSeriesPostgresFromTVDBID(tvdbId);
    } else {
      throw new RuntimeException("Found MongoDB series with null tivo id and tvdb id.");
    }
  }

  private void updateEpisodes(SeriesMongo seriesMongo, Series series) throws SQLException {
    ObjectId mongoId = seriesMongo._id.getValue();

    BasicDBObject episodeQuery = new BasicDBObject()
        .append("SeriesId", mongoId)
        ;

    DBCursor cursor = mongoConnection.getCollection("episodes").find(episodeQuery);

    debug(" - Updating " + cursor.count() + " episodes.");

    while (cursor.hasNext()) {
      DBObject episodeDBObj = cursor.next();

      try {
        updateSingleEpisode(series, episodeDBObj);
      } catch (ShowFailedException e) {
        debug("Failed!");
        //noinspection ThrowableResultOfMethodCallIgnored
        failedEpisodes.put((ObjectId) episodeDBObj.get("_id"), e);
      }
    }
  }

  private void updateSingleEpisode(Series series, DBObject episodeDBObj) throws SQLException, ShowFailedException {
    EpisodeMongo episodeMongo = new EpisodeMongo();
    episodeMongo.initializeFromDBObject(episodeDBObj);

    Integer tvdbNativeEpisodeId = episodeMongo.tvdbEpisodeId.getValue();
    String tivoNativeEpisodeId = episodeMongo.tivoProgramId.getValue();
    String tvdbInfo = (tvdbNativeEpisodeId == null) ? " (NO TVDB!)" : "";
    String tivoInfo = (tivoNativeEpisodeId == null) ? " (NO TiVo!)" : "";

    TiVoEpisode tiVoEpisode = insertTiVoEpisode(episodeMongo, tivoNativeEpisodeId);

    if (tvdbNativeEpisodeId != null) {
      Integer tvdbLocalEpisodeId = insertTVDBEpisodeAndReturnId(episodeMongo, tvdbNativeEpisodeId, series.tvdbSeriesId.getValue());

      Episode episode = new Episode();
      episode.initializeForInsert();

      if (episode.id.getValue() == null) {
        debug("    * " + episodeMongo + " (INSERT)" + tvdbInfo + tivoInfo);
      } else {
        debug("    * " + episodeMongo + " (UPDATE)" + tvdbInfo + tivoInfo);
      }

      episode.seriesId.changeValue(series.id.getValue());
      episode.tvdbEpisodeId.changeValue(tvdbLocalEpisodeId);

      updateSeasonNumber(series, episodeMongo, episode);

      copyAllEpisodeFields(episodeMongo, episode);
      episode.commit(sqlConnection);

      if (tiVoEpisode != null) {
        episode.addToTiVoEpisodes(sqlConnection, tiVoEpisode);
      }

      updateRetired(episodeMongo, episode);
    }

  }

  private void updateRetired(EpisodeMongo episodeMongo, Episode episode) throws SQLException {
    if (episodeMongo.matchingStump.getValue()) {
      Integer id_after_insert = episode.id.getValue();
      if (id_after_insert == null) {
        throw new RuntimeException("Episode should have id after insert.");
      }

      episode.retired.changeValue(id_after_insert);
      episode.commit(sqlConnection);
    }
  }

  private void updateSeasonNumber(Series series, EpisodeMongo episodeMongo, Episode episode) throws SQLException {
    Integer seasonNumber = episodeMongo.tvdbSeason.getValue();
    if (seasonNumber != null) {
      Season season = series.getOrCreateSeason(sqlConnection, seasonNumber);
      episode.seasonId.changeValue(season.id.getValue());
    }
  }

  private Integer insertTVDBEpisodeAndReturnId(EpisodeMongo episodeMongo, Integer tvdbNativeEpisodeId, Integer tvdbSeriesId) throws SQLException, ShowFailedException {
    if (tvdbNativeEpisodeId == null) {
      return null;
    }
    TVDBEpisode tvdbEpisode = getOrCreateTVDBEpisodePostgres(tvdbNativeEpisodeId);

    copyAllTVDBEpisodeFields(episodeMongo, tvdbEpisode);
    tvdbEpisode.tvdbSeriesId.changeValue(tvdbSeriesId);
    tvdbEpisode.commit(sqlConnection);

    Integer tvdbLocalEpisodeId = tvdbEpisode.id.getValue();

    if (tvdbLocalEpisodeId == null) {
      throw new RuntimeException("No ID populated on tvdb_episode postgres object after insert or update.");
    }
    return tvdbLocalEpisodeId;
  }

  @Nullable
  private TiVoEpisode insertTiVoEpisode(EpisodeMongo episodeMongo, String tivoNativeEpisodeId) throws SQLException, ShowFailedException {
    if (tivoNativeEpisodeId == null) {
      return null;
    }

    TiVoEpisode tiVoEpisode = getOrCreateTiVoEpisodePostgres(tivoNativeEpisodeId, episodeMongo.matchingStump.getValue() ? 1 : 0);

    copyAllTiVoEpisodeFields(episodeMongo, tiVoEpisode);
    tiVoEpisode.commit(sqlConnection);

    Integer tivoLocalEpisodeId = tiVoEpisode.id.getValue();
    if (tivoLocalEpisodeId == null) {
      throw new RuntimeException("No ID populated on tivo_episode postgres object after insert or update.");
    }

    if (episodeMongo.matchingStump.getValue()) {
      tiVoEpisode.retired.changeValue(tivoLocalEpisodeId);
      tiVoEpisode.commit(sqlConnection);
    }

    return tiVoEpisode;
  }

  private void copyAllEpisodeFields(EpisodeMongo episodeMongo, Episode episode) {
    episode.watchedDate.changeValueUnlessToNull(episodeMongo.watchedDate.getValue());
    episode.onTiVo.changeValueUnlessToNull(episodeMongo.onTiVo.getValue());
    episode.watched.changeValueUnlessToNull(episodeMongo.watched.getValue());

    episode.seriesTitle.changeValueUnlessToNull(episodeMongo.tivoSeriesTitle.getValue());

    String tivoTitle = episodeMongo.tivoEpisodeTitle.getValue();
    String tvdbTitle = episodeMongo.tvdbEpisodeName.getValue();
    episode.title.changeValueUnlessToNull(tvdbTitle == null ? tivoTitle : tvdbTitle);

    episode.season.changeValueUnlessToNull(episodeMongo.tvdbSeason.getValue());
    episode.seasonEpisodeNumber.changeValueUnlessToNull(episodeMongo.tvdbEpisodeNumber.getValue());
    episode.episodeNumber.changeValueUnlessToNull(episodeMongo.tvdbAbsoluteNumber.getValue());
    episode.airDate.changeValueUnlessToNull(episodeMongo.tvdbFirstAired.getValue());

    episode.dateAdded.changeValue(episodeMongo.dateAdded.getValue());

    episode.retired.changeValueUnlessToNull(episodeMongo.matchingStump.getValue() ? 1 : 0);
  }

  private void copyAllTiVoEpisodeFields(EpisodeMongo episodeMongo, TiVoEpisode tiVoEpisode) {
    tiVoEpisode.suggestion.changeValueUnlessToNull(episodeMongo.tivoSuggestion.getValue());
    tiVoEpisode.title.changeValueUnlessToNull(episodeMongo.tivoEpisodeTitle.getValue());
    tiVoEpisode.showingStartTime.changeValueUnlessToNull(episodeMongo.tivoShowingStartTime.getValue());
    tiVoEpisode.showingDuration.changeValueUnlessToNull(episodeMongo.tivoShowingDuration.getValue());
    tiVoEpisode.deletedDate.changeValueUnlessToNull(episodeMongo.tivoDeletedDate.getValue());
    tiVoEpisode.captureDate.changeValueUnlessToNull(episodeMongo.tivoCaptureDate.getValue());
    tiVoEpisode.hd.changeValueUnlessToNull(episodeMongo.tivoHD.getValue());
    tiVoEpisode.episodeNumber.changeValueUnlessToNull(episodeMongo.tivoEpisodeNumber.getValue());
    tiVoEpisode.duration.changeValueUnlessToNull(episodeMongo.tivoDuration.getValue());
    tiVoEpisode.channel.changeValueUnlessToNull(episodeMongo.tivoChannel.getValue());
    tiVoEpisode.rating.changeValueUnlessToNull(episodeMongo.tivoRating.getValue());
    tiVoEpisode.tivoSeriesId.changeValueUnlessToNull(episodeMongo.tivoSeriesId.getValue());
    tiVoEpisode.programId.changeValueUnlessToNull(episodeMongo.tivoProgramId.getValue());
    tiVoEpisode.description.changeValueUnlessToNull(episodeMongo.tivoDescription.getValue());
    tiVoEpisode.station.changeValueUnlessToNull(episodeMongo.tivoStation.getValue());
    tiVoEpisode.url.changeValueUnlessToNull(episodeMongo.tivoUrl.getValue());
    tiVoEpisode.seriesTitle.changeValueUnlessToNull(episodeMongo.tivoSeriesTitle.getValue());
    tiVoEpisode.dateAdded.changeValue(episodeMongo.dateAdded.getValue());

    tiVoEpisode.retired.changeValueUnlessToNull(episodeMongo.matchingStump.getValue() ? 1 : 0);
  }

  private void copyAllTVDBEpisodeFields(EpisodeMongo episodeMongo, TVDBEpisode tvdbEpisode) {
    tvdbEpisode.seasonNumber.changeValue(episodeMongo.tvdbSeason.getValue());
    tvdbEpisode.seasonId.changeValue(episodeMongo.tvdbSeasonId.getValue());
    tvdbEpisode.tvdbId.changeValue(episodeMongo.tvdbEpisodeId.getValue());
    tvdbEpisode.episodeNumber.changeValue(episodeMongo.tvdbEpisodeNumber.getValue());
    tvdbEpisode.absoluteNumber.changeValue(episodeMongo.tvdbAbsoluteNumber.getValue());
    tvdbEpisode.ratingCount.changeValue(episodeMongo.tvdbRatingCount.getValue());
    tvdbEpisode.airsAfterSeason.changeValue(episodeMongo.tvdbAirsAfterSeason.getValue());
    tvdbEpisode.airsBeforeSeason.changeValue(episodeMongo.tvdbAirsBeforeSeason.getValue());
    tvdbEpisode.airsBeforeEpisode.changeValue(episodeMongo.tvdbAirsBeforeEpisode.getValue());
    tvdbEpisode.thumbHeight.changeValue(episodeMongo.tvdbThumbHeight.getValue());
    tvdbEpisode.thumbWidth.changeValue(episodeMongo.tvdbThumbWidth.getValue());
    tvdbEpisode.firstAired.changeValue(episodeMongo.tvdbFirstAired.getValue());
    tvdbEpisode.lastUpdated.changeValue(episodeMongo.tvdbLastUpdated.getValue());
    tvdbEpisode.rating.changeValue(episodeMongo.tvdbRating.getValue());
    tvdbEpisode.seriesName.changeValue(episodeMongo.tvdbSeriesName.getValue());
    tvdbEpisode.name.changeValue(episodeMongo.tvdbEpisodeName.getValue());
    tvdbEpisode.overview.changeValue(episodeMongo.tvdbOverview.getValue());
    tvdbEpisode.productionCode.changeValue(episodeMongo.tvdbProductionCode.getValue());
    tvdbEpisode.director.changeValue(episodeMongo.tvdbDirector.getValue());
    tvdbEpisode.writer.changeValue(episodeMongo.tvdbWriter.getValue());
    tvdbEpisode.filename.changeValue(episodeMongo.tvdbFilename.getValue());
    tvdbEpisode.dateAdded.changeValue(episodeMongo.dateAdded.getValue());
  }

  private void copyAllTVDBSeriesFields(SeriesMongo seriesMongo, TVDBSeries tvdbSeries) {
    tvdbSeries.firstAired.changeValue(seriesMongo.tvdbFirstAired.getValue());
    tvdbSeries.tvdbId.changeValue(seriesMongo.tvdbId.getValue());
    tvdbSeries.tvdbSeriesId.changeValue(seriesMongo.tvdbSeriesId.getValue());
    tvdbSeries.ratingCount.changeValue(seriesMongo.tvdbRatingCount.getValue());
    tvdbSeries.runtime.changeValue(seriesMongo.tvdbRuntime.getValue());
    tvdbSeries.rating.changeValue(seriesMongo.tvdbRating.getValue());
    tvdbSeries.name.changeValue(seriesMongo.tvdbName.getValue());
    tvdbSeries.airsDayOfWeek.changeValue(seriesMongo.tvdbAirsDayOfWeek.getValue());
    tvdbSeries.airsTime.changeValue(seriesMongo.tvdbAirsTime.getValue());
    tvdbSeries.network.changeValue(seriesMongo.tvdbNetwork.getValue());
    tvdbSeries.overview.changeValue(seriesMongo.tvdbOverview.getValue());
    tvdbSeries.status.changeValue(seriesMongo.tvdbStatus.getValue());
    tvdbSeries.poster.changeValue(seriesMongo.tvdbPoster.getValue());
    tvdbSeries.banner.changeValue(seriesMongo.tvdbBanner.getValue());
    tvdbSeries.lastUpdated.changeValue(seriesMongo.tvdbLastUpdated.getValue());
    tvdbSeries.imdbId.changeValue(seriesMongo.imdbId.getValue());
    tvdbSeries.zap2it_id.changeValue(seriesMongo.zap2it_id.getValue());
    tvdbSeries.dateAdded.changeValue(seriesMongo.dateAdded.getValue());
  }

  private void copyAllSeriesFields(SeriesMongo seriesMongo, Series series) {
    series.tier.changeValue(seriesMongo.tier.getValue());
    series.tivoSeriesId.changeValue(seriesMongo.tivoSeriesId.getValue());
    series.tvdbId.changeValue(seriesMongo.tvdbId.getValue());
    series.seriesTitle.changeValue(seriesMongo.seriesTitle.getValue());
    series.tivoName.changeValue(seriesMongo.tivoName.getValue());
    series.tvdbHint.changeValue(seriesMongo.tvdbHint.getValue());
    series.ignoreTVDB.changeValue(seriesMongo.ignoreTVDB.getValue());
    series.activeEpisodes.changeValue(seriesMongo.activeEpisodes.getValue());
    series.deletedEpisodes.changeValue(seriesMongo.deletedEpisodes.getValue());
    series.suggestionEpisodes.changeValue(seriesMongo.suggestionEpisodes.getValue());
    series.unmatchedEpisodes.changeValue(seriesMongo.unmatchedEpisodes.getValue());
    series.watchedEpisodes.changeValue(seriesMongo.watchedEpisodes.getValue());
    series.unwatchedEpisodes.changeValue(seriesMongo.unwatchedEpisodes.getValue());
    series.unwatchedUnrecorded.changeValue(seriesMongo.unwatchedUnrecorded.getValue());
    series.tvdbOnlyEpisodes.changeValue(seriesMongo.tvdbOnlyEpisodes.getValue());
    series.matchedEpisodes.changeValue(seriesMongo.matchedEpisodes.getValue());
    series.metacritic.changeValue(seriesMongo.metacritic.getValue());
    series.metacriticHint.changeValue(seriesMongo.metacriticHint.getValue());
    series.lastUnwatched.changeValue(seriesMongo.lastUnwatched.getValue());
    series.mostRecent.changeValue(seriesMongo.mostRecent.getValue());
    series.isSuggestion.changeValue(seriesMongo.isSuggestion.getValue());
    series.matchedWrong.changeValue(seriesMongo.matchedWrong.getValue());
    series.needsTVDBRedo.changeValue(seriesMongo.needsTVDBRedo.getValue());
    series.my_rating.changeValue(seriesMongo.myRating.getValue());
    series.dateAdded.changeValue(seriesMongo.dateAdded.getValue());
  }

  private Series getOrCreateSeriesPostgresFromTiVoID(String tivoSeriesId) throws SQLException {
    Series series = new Series();
    if (tivoSeriesId == null) {
      series.initializeForInsert();
      return series;
    }

    String sql = "SELECT * FROM series WHERE tivo_series_id = ?";
    ResultSet resultSet = sqlConnection.prepareAndExecuteStatementFetch(sql, tivoSeriesId);

    if (resultSet.next()) {
      if (devMode) {
        throw new RuntimeException("DEV MODE: Expect to never update. Found series already with existing TiVoSeriesID: " + tivoSeriesId);
      }
      series.initializeFromDBObject(resultSet);
    } else {
      series.initializeForInsert();
    }
    return series;
  }

  private Series getOrCreateSeriesPostgresFromTVDBID(Integer tvdbId) throws SQLException {
    Series series = new Series();
    if (tvdbId == null) {
      series.initializeForInsert();
      return series;
    }

    String sql = "SELECT * FROM series WHERE tvdb_id = ?";
    ResultSet resultSet = sqlConnection.prepareAndExecuteStatementFetch(sql, tvdbId);

    if (resultSet.next()) {
      if (devMode) {
        throw new RuntimeException("DEV MODE: Expect to never update. Found series already with existing TVDB ID: " + tvdbId);
      }
      series.initializeFromDBObject(resultSet);
    } else {
      series.initializeForInsert();
    }
    return series;
  }

  private TiVoEpisode getOrCreateTiVoEpisodePostgres(String tivoProgramId, Integer retired) throws SQLException, ShowFailedException {
    TiVoEpisode tiVoEpisode = new TiVoEpisode();

    if (tivoProgramId == null) {
      tiVoEpisode.initializeForInsert();
      return tiVoEpisode;
    }

    String sql = "SELECT * FROM tivo_episode WHERE program_id = ? and retired = ?";
    ResultSet resultSet = sqlConnection.prepareAndExecuteStatementFetch(sql, tivoProgramId, retired);

    if (resultSet.next()) {
      tiVoEpisode.initializeFromDBObject(resultSet);
    } else {
      tiVoEpisode.initializeForInsert();
    }
    return tiVoEpisode;
  }

  private TVDBEpisode getOrCreateTVDBEpisodePostgres(Integer tvdbEpisodeId) throws SQLException, ShowFailedException {
    TVDBEpisode tvdbEpisode = new TVDBEpisode();
    if (tvdbEpisodeId == null) {
      tvdbEpisode.initializeForInsert();
      return tvdbEpisode;
    }

    String sql = "SELECT * FROM tvdb_episode WHERE tvdb_id = ?";
    ResultSet resultSet = sqlConnection.prepareAndExecuteStatementFetch(sql, tvdbEpisodeId);

    if (resultSet.next()) {
      tvdbEpisode.initializeFromDBObject(resultSet);
      if (devMode) {
        throw new ShowFailedException("DEV MODE: Expect to never update. " +
            "Found tvdb_episode already with existing TVDB ID: " + tvdbEpisodeId +
            ", " + tvdbEpisode);
      }
    } else {
      tvdbEpisode.initializeForInsert();
    }
    return tvdbEpisode;
  }

  private TVDBSeries getOrCreateTVDBSeriesPostgres(Integer tvdbId) throws SQLException {
    TVDBSeries tvdbSeries = new TVDBSeries();
    if (tvdbId == null) {
      tvdbSeries.initializeForInsert();
      return tvdbSeries;
    }

    String sql = "SELECT * FROM tvdb_series WHERE tvdb_id = ?";
    ResultSet resultSet = sqlConnection.prepareAndExecuteStatementFetch(sql, tvdbId);

    if (resultSet.next()) {
      tvdbSeries.initializeFromDBObject(resultSet);
    } else {
      tvdbSeries.initializeForInsert();
    }
    return tvdbSeries;
  }

  protected void debug(Object object) {
    System.out.println(object);
  }

}
