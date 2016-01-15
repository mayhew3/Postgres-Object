package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.games.MongoConnection;
import com.mayhew3.gamesutil.games.PostgresConnection;
import com.mayhew3.gamesutil.mediaobject.*;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TVPostgresMigration {
  private static MongoConnection mongoConnection;
  private static PostgresConnection postgresConnection;

  private static Boolean devMode = false;

  public static void main(String[] args) throws SQLException {
    List<String> argList = Lists.newArrayList(args);
    devMode = argList.contains("dev");

    postgresConnection = new PostgresConnection();
    mongoConnection = new MongoConnection("tv");

    TVPostgresMigration tvPostgresMigration = new TVPostgresMigration();

    if (devMode) {
      tvPostgresMigration.truncatePostgresTables();
    }

    tvPostgresMigration.updatePostgresDatabase();
  }

  private void truncatePostgresTables() throws SQLException {
    postgresConnection.executeUpdate("TRUNCATE TABLE tvdb_series CASCADE");
    postgresConnection.executeUpdate("TRUNCATE TABLE tvdb_episode CASCADE");
    postgresConnection.executeUpdate("TRUNCATE TABLE tivo_episode CASCADE");
    postgresConnection.executeUpdate("TRUNCATE TABLE genre CASCADE");
    postgresConnection.executeUpdate("TRUNCATE TABLE viewing_location CASCADE");
    postgresConnection.executeUpdate("TRUNCATE TABLE edge_tivo_episode CASCADE");

    postgresConnection.executeUpdate("ALTER SEQUENCE series_id_seq RESTART WITH 1");
    postgresConnection.executeUpdate("ALTER SEQUENCE tvdb_series_id_seq RESTART WITH 1");
    postgresConnection.executeUpdate("ALTER SEQUENCE season_id_seq RESTART WITH 1");
    postgresConnection.executeUpdate("ALTER SEQUENCE episode_id_seq RESTART WITH 1");
    postgresConnection.executeUpdate("ALTER SEQUENCE tivo_episode_id_seq RESTART WITH 1");
    postgresConnection.executeUpdate("ALTER SEQUENCE tvdb_episode_id_seq RESTART WITH 1");

    postgresConnection.executeUpdate("ALTER SEQUENCE genre_id_seq RESTART WITH 1");
    postgresConnection.executeUpdate("ALTER SEQUENCE series_genre_id_seq RESTART WITH 1");

    postgresConnection.executeUpdate("ALTER SEQUENCE viewing_location_id_seq RESTART WITH 1");
    postgresConnection.executeUpdate("ALTER SEQUENCE series_viewing_location_id_seq RESTART WITH 1");

  }

  public void updatePostgresDatabase() throws SQLException {
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


  private void updateSingleSeries(DBObject seriesMongoObject) throws SQLException {
    SeriesMongo seriesMongo = new SeriesMongo();
    seriesMongo.initializeFromDBObject(seriesMongoObject);

    SeriesPostgres seriesPostgres = getOrCreateSeriesPostgres(seriesMongo);

    String title = seriesMongo.seriesTitle.getValue();

    if (seriesPostgres.id.getValue() != null) {
      debug(title + ": Updating");
    } else {
      debug(title + ": Inserting");
    }

    Integer tvdbSeriesId = updateTVDBSeries(seriesMongo);

    seriesPostgres.tvdbSeriesId.changeValue(tvdbSeriesId);
    copyAllSeriesFields(seriesMongo, seriesPostgres);
    seriesPostgres.commit(postgresConnection);

    Integer seriesId = seriesPostgres.id.getValue();
    if (seriesId == null) {
      throw new RuntimeException("No ID populated on series postgres object after insert or update.");
    }

    updateGenres(seriesMongo, seriesPostgres);
    updateViewingLocations(seriesMongo, seriesPostgres);
    updateMetacriticSeasons(seriesMongo, seriesPostgres);
    updateEpisodes(seriesMongo, seriesPostgres);
  }


  private Integer updateTVDBSeries(SeriesMongo seriesMongo) throws SQLException {
    if (seriesMongo.tvdbId.getValue() == null) {
      return null;
    }

    debug("   (also copying tvdb info...)");

    TVDBSeriesPostgres tvdbSeriesPostgres = getOrCreateTVDBSeriesPostgres(seriesMongo.tvdbId.getValue());

    copyAllTVDBSeriesFields(seriesMongo, tvdbSeriesPostgres);
    tvdbSeriesPostgres.commit(postgresConnection);

    Integer tvdbSeriesId = tvdbSeriesPostgres.id.getValue();

    if (tvdbSeriesId == null) {
      throw new RuntimeException("No ID populated on tvdb_series postgres object after insert or update.");
    }
    return tvdbSeriesId;
  }

  private void updateMetacriticSeasons(SeriesMongo seriesMongo, SeriesPostgres seriesPostgres) throws SQLException {
    BasicDBList metacriticSeasons = seriesMongo.metacriticSeasons.getValue();
    if (metacriticSeasons != null) {
      for (Object obj : metacriticSeasons) {
        DBObject dbObject = (DBObject) obj;
        Integer seasonNumber = (Integer) dbObject.get("SeasonNumber");
        Integer seasonMetacritic = (Integer) dbObject.get("SeasonMetacritic");

        debug(" - Add season " + seasonNumber + " with Metacritic " + seasonMetacritic);

        SeasonPostgres season = seriesPostgres.getOrCreateSeason(postgresConnection, seasonNumber);
        season.metacritic.changeValue(seasonMetacritic);
        season.commit(postgresConnection);
      }
    }
  }

  private void updateViewingLocations(SeriesMongo seriesMongo, SeriesPostgres seriesPostgres) throws SQLException {
    BasicDBList viewingLocations = seriesMongo.viewingLocations.getValue();
    if (viewingLocations != null) {
      for (Object obj : viewingLocations) {
        String viewingLocation = (String) obj;
        debug(" - Add viewing location '" + viewingLocation + "'");
        seriesPostgres.addViewingLocation(postgresConnection, viewingLocation);
      }
    }
  }

  private void updateGenres(SeriesMongo seriesMongo, SeriesPostgres seriesPostgres) throws SQLException {
    BasicDBList tvdbGenre = seriesMongo.tvdbGenre.getValue();
    if (tvdbGenre != null) {
      for (Object obj : tvdbGenre) {
        String genreName = (String) obj;
        debug(" - Add genre '" + genreName + "'");
        seriesPostgres.addGenre(postgresConnection, genreName);
      }
    }
  }

  private SeriesPostgres getOrCreateSeriesPostgres(SeriesMongo seriesMongo) throws SQLException {
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

  private void updateEpisodes(SeriesMongo seriesMongo, SeriesPostgres seriesPostgres) throws SQLException {
    ObjectId mongoId = seriesMongo._id.getValue();

    BasicDBObject episodeQuery = new BasicDBObject()
        .append("SeriesId", mongoId)
        ;

    DBCursor cursor = mongoConnection.getCollection("episodes").find(episodeQuery);

    debug(" - Updating " + cursor.count() + " episodes.");

    while (cursor.hasNext()) {
      DBObject episodeDBObj = cursor.next();

      updateSingleEpisode(seriesPostgres, episodeDBObj);
    }
  }

  private void updateSingleEpisode(SeriesPostgres seriesPostgres, DBObject episodeDBObj) throws SQLException {
    EpisodeMongo episodeMongo = new EpisodeMongo();
    episodeMongo.initializeFromDBObject(episodeDBObj);

    EpisodePostgres episodePostgres = getOrCreateEpisodePostgres(episodeMongo.tivoProgramId.getValue());

    Integer tvdbNativeEpisodeId = episodeMongo.tvdbEpisodeId.getValue();
    String tivoNativeEpisodeId = episodeMongo.tivoProgramId.getValue();
    String tvdbInfo = (tvdbNativeEpisodeId == null) ? " (NO TVDB!)" : "";
    String tivoInfo = (tivoNativeEpisodeId == null) ? " (NO TiVo!)" : "";

    if (episodePostgres.id.getValue() == null) {
      debug("    * " + episodeMongo + " (INSERT)" + tvdbInfo + tivoInfo);
    } else {
      debug("    * " + episodeMongo + " (UPDATE)" + tvdbInfo + tivoInfo);
    }

    Integer tivoLocalEpisodeId = insertTiVoEpisodeAndReturnId(episodeMongo, tivoNativeEpisodeId);
    Integer tvdbLocalEpisodeId = insertTVDBEpisodeAndReturnId(episodeMongo, tvdbNativeEpisodeId);

    updateSeasonNumber(seriesPostgres, episodeMongo, episodePostgres);

    episodePostgres.seriesId.changeValue(seriesPostgres.id.getValue());
    episodePostgres.tivoEpisodeId.changeValue(tivoLocalEpisodeId);
    episodePostgres.tvdbEpisodeId.changeValue(tvdbLocalEpisodeId);

    copyAllEpisodeFields(episodeMongo, episodePostgres);
    episodePostgres.commit(postgresConnection);

    if (tivoLocalEpisodeId != null) {
      episodePostgres.addToTiVoEpisodes(postgresConnection, tivoLocalEpisodeId);
    }

    updateRetired(episodeMongo, episodePostgres);
  }

  private void updateRetired(EpisodeMongo episodeMongo, EpisodePostgres episodePostgres) {
    if (episodeMongo.matchingStump.getValue()) {
      Integer id_after_insert = episodePostgres.id.getValue();
      if (id_after_insert == null) {
        throw new RuntimeException("Episode should have id after insert.");
      }

      episodePostgres.retired.changeValue(id_after_insert);
      episodePostgres.commit(postgresConnection);
    }
  }

  private void updateSeasonNumber(SeriesPostgres seriesPostgres, EpisodeMongo episodeMongo, EpisodePostgres episodePostgres) throws SQLException {
    Integer seasonNumber = episodeMongo.tvdbSeason.getValue();
    if (seasonNumber != null) {
      SeasonPostgres season = seriesPostgres.getOrCreateSeason(postgresConnection, seasonNumber);
      episodePostgres.seasonId.changeValue(season.id.getValue());
    }
  }

  private Integer insertTVDBEpisodeAndReturnId(EpisodeMongo episodeMongo, Integer tvdbNativeEpisodeId) throws SQLException {
    if (tvdbNativeEpisodeId == null) {
      return null;
    }
    TVDBEpisodePostgres tvdbEpisodePostgres = getOrCreateTVDBEpisodePostgres(tvdbNativeEpisodeId);

    copyAllTVDBEpisodeFields(episodeMongo, tvdbEpisodePostgres);
    tvdbEpisodePostgres.commit(postgresConnection);

    Integer tvdbLocalEpisodeId = tvdbEpisodePostgres.id.getValue();

    if (tvdbLocalEpisodeId == null) {
      throw new RuntimeException("No ID populated on tvdb_episode postgres object after insert or update.");
    }
    return tvdbLocalEpisodeId;
  }

  private Integer insertTiVoEpisodeAndReturnId(EpisodeMongo episodeMongo, String tivoNativeEpisodeId) throws SQLException {
    if (tivoNativeEpisodeId == null) {
      return null;
    }

    TiVoEpisodePostgres tiVoEpisodePostgres = getOrCreateTiVoEpisodePostgres(tivoNativeEpisodeId);

    copyAllTiVoEpisodeFields(episodeMongo, tiVoEpisodePostgres);
    tiVoEpisodePostgres.commit(postgresConnection);

    Integer tivoLocalEpisodeId = tiVoEpisodePostgres.id.getValue();
    if (tivoLocalEpisodeId == null) {
      throw new RuntimeException("No ID populated on tivo_episode postgres object after insert or update.");
    }

    if (episodeMongo.matchingStump.getValue()) {
      tiVoEpisodePostgres.retired.changeValue(tivoLocalEpisodeId);
      tiVoEpisodePostgres.commit(postgresConnection);
    }

    return tivoLocalEpisodeId;
  }

  private void copyAllEpisodeFields(EpisodeMongo episodeMongo, EpisodePostgres episodePostgres) {
    episodePostgres.watchedDate.changeValue(episodeMongo.watchedDate.getValue());
    episodePostgres.onTiVo.changeValue(episodeMongo.onTiVo.getValue());
    episodePostgres.watched.changeValue(episodeMongo.watched.getValue());

    episodePostgres.seriesTitle.changeValue(episodeMongo.tivoSeriesTitle.getValue());
    episodePostgres.tivoProgramId.changeValue(episodeMongo.tivoProgramId.getValue());

    String tivoTitle = episodeMongo.tivoEpisodeTitle.getValue();
    String tvdbTitle = episodeMongo.tvdbEpisodeName.getValue();
    episodePostgres.title.changeValue(tvdbTitle == null ? tivoTitle : tvdbTitle);

    episodePostgres.season.changeValue(episodeMongo.tvdbSeason.getValue());
    episodePostgres.seasonEpisodeNumber.changeValue(episodeMongo.tvdbEpisodeNumber.getValue());
    episodePostgres.episodeNumber.changeValue(episodeMongo.tvdbAbsoluteNumber.getValue());
    episodePostgres.airDate.changeValue(episodeMongo.tvdbFirstAired.getValue());

    episodePostgres.retired.changeValue(episodeMongo.matchingStump.getValue() ? 1 : 0);
  }

  private void copyAllTiVoEpisodeFields(EpisodeMongo episodeMongo, TiVoEpisodePostgres tiVoEpisodePostgres) {
    tiVoEpisodePostgres.suggestion.changeValue(episodeMongo.tivoSuggestion.getValue());
    tiVoEpisodePostgres.title.changeValue(episodeMongo.tivoEpisodeTitle.getValue());
    tiVoEpisodePostgres.showingStartTime.changeValue(episodeMongo.tivoShowingStartTime.getValue());
    tiVoEpisodePostgres.showingDuration.changeValue(episodeMongo.tivoShowingDuration.getValue());
    tiVoEpisodePostgres.deletedDate.changeValue(episodeMongo.tivoDeletedDate.getValue());
    tiVoEpisodePostgres.captureDate.changeValue(episodeMongo.tivoCaptureDate.getValue());
    tiVoEpisodePostgres.hd.changeValue(episodeMongo.tivoHD.getValue());
    tiVoEpisodePostgres.episodeNumber.changeValue(episodeMongo.tivoEpisodeNumber.getValue());
    tiVoEpisodePostgres.duration.changeValue(episodeMongo.tivoDuration.getValue());
    tiVoEpisodePostgres.channel.changeValue(episodeMongo.tivoChannel.getValue());
    tiVoEpisodePostgres.rating.changeValue(episodeMongo.tivoRating.getValue());
    tiVoEpisodePostgres.tivoSeriesId.changeValue(episodeMongo.tivoSeriesId.getValue());
    tiVoEpisodePostgres.programId.changeValue(episodeMongo.tivoProgramId.getValue());
    tiVoEpisodePostgres.description.changeValue(episodeMongo.tivoDescription.getValue());
    tiVoEpisodePostgres.station.changeValue(episodeMongo.tivoStation.getValue());
    tiVoEpisodePostgres.url.changeValue(episodeMongo.tivoUrl.getValue());
    tiVoEpisodePostgres.seriesTitle.changeValue(episodeMongo.tivoSeriesTitle.getValue());

    tiVoEpisodePostgres.retired.changeValue(episodeMongo.matchingStump.getValue() ? 1 : 0);
  }

  private void copyAllTVDBEpisodeFields(EpisodeMongo episodeMongo, TVDBEpisodePostgres tvdbEpisodePostgres) {
    tvdbEpisodePostgres.seasonNumber.changeValue(episodeMongo.tvdbSeason.getValue());
    tvdbEpisodePostgres.seasonId.changeValue(episodeMongo.tvdbSeasonId.getValue());
    tvdbEpisodePostgres.tvdbId.changeValue(episodeMongo.tvdbEpisodeId.getValue());
    tvdbEpisodePostgres.episodeNumber.changeValue(episodeMongo.tvdbEpisodeNumber.getValue());
    tvdbEpisodePostgres.absoluteNumber.changeValue(episodeMongo.tvdbAbsoluteNumber.getValue());
    tvdbEpisodePostgres.ratingCount.changeValue(episodeMongo.tvdbRatingCount.getValue());
    tvdbEpisodePostgres.airsAfterSeason.changeValue(episodeMongo.tvdbAirsAfterSeason.getValue());
    tvdbEpisodePostgres.airsBeforeSeason.changeValue(episodeMongo.tvdbAirsBeforeSeason.getValue());
    tvdbEpisodePostgres.airsBeforeEpisode.changeValue(episodeMongo.tvdbAirsBeforeEpisode.getValue());
    tvdbEpisodePostgres.thumbHeight.changeValue(episodeMongo.tvdbThumbHeight.getValue());
    tvdbEpisodePostgres.thumbWidth.changeValue(episodeMongo.tvdbThumbWidth.getValue());
    tvdbEpisodePostgres.firstAired.changeValue(episodeMongo.tvdbFirstAired.getValue());
    tvdbEpisodePostgres.lastUpdated.changeValue(episodeMongo.tvdbLastUpdated.getValue());
    tvdbEpisodePostgres.rating.changeValue(episodeMongo.tvdbRating.getValue());
    tvdbEpisodePostgres.seriesName.changeValue(episodeMongo.tvdbSeriesName.getValue());
    tvdbEpisodePostgres.name.changeValue(episodeMongo.tvdbEpisodeName.getValue());
    tvdbEpisodePostgres.overview.changeValue(episodeMongo.tvdbOverview.getValue());
    tvdbEpisodePostgres.productionCode.changeValue(episodeMongo.tvdbProductionCode.getValue());
    tvdbEpisodePostgres.director.changeValue(episodeMongo.tvdbDirector.getValue());
    tvdbEpisodePostgres.writer.changeValue(episodeMongo.tvdbWriter.getValue());
    tvdbEpisodePostgres.filename.changeValue(episodeMongo.tvdbFilename.getValue());
  }

  private void copyAllTVDBSeriesFields(SeriesMongo seriesMongo, TVDBSeriesPostgres tvdbSeriesPostgres) {
    tvdbSeriesPostgres.firstAired.changeValue(seriesMongo.tvdbFirstAired.getValue());
    tvdbSeriesPostgres.tvdbId.changeValue(seriesMongo.tvdbId.getValue());
    tvdbSeriesPostgres.tvdbSeriesId.changeValue(seriesMongo.tvdbSeriesId.getValue());
    tvdbSeriesPostgres.ratingCount.changeValue(seriesMongo.tvdbRatingCount.getValue());
    tvdbSeriesPostgres.runtime.changeValue(seriesMongo.tvdbRuntime.getValue());
    tvdbSeriesPostgres.rating.changeValue(seriesMongo.tvdbRating.getValue());
    tvdbSeriesPostgres.name.changeValue(seriesMongo.tvdbName.getValue());
    tvdbSeriesPostgres.airsDayOfWeek.changeValue(seriesMongo.tvdbAirsDayOfWeek.getValue());
    tvdbSeriesPostgres.airsTime.changeValue(seriesMongo.tvdbAirsTime.getValue());
    tvdbSeriesPostgres.network.changeValue(seriesMongo.tvdbNetwork.getValue());
    tvdbSeriesPostgres.overview.changeValue(seriesMongo.tvdbOverview.getValue());
    tvdbSeriesPostgres.status.changeValue(seriesMongo.tvdbStatus.getValue());
    tvdbSeriesPostgres.poster.changeValue(seriesMongo.tvdbPoster.getValue());
    tvdbSeriesPostgres.banner.changeValue(seriesMongo.tvdbBanner.getValue());
    tvdbSeriesPostgres.lastUpdated.changeValue(seriesMongo.tvdbLastUpdated.getValue());
    tvdbSeriesPostgres.imdbId.changeValue(seriesMongo.imdbId.getValue());
    tvdbSeriesPostgres.zap2it_id.changeValue(seriesMongo.zap2it_id.getValue());
  }

  private void copyAllSeriesFields(SeriesMongo seriesMongo, SeriesPostgres seriesPostgres) {
    seriesPostgres.tier.changeValue(seriesMongo.tier.getValue());
    seriesPostgres.tivoSeriesId.changeValue(seriesMongo.tivoSeriesId.getValue());
    seriesPostgres.tvdbId.changeValue(seriesMongo.tvdbId.getValue());
    seriesPostgres.seriesTitle.changeValue(seriesMongo.seriesTitle.getValue());
    seriesPostgres.tvdbHint.changeValue(seriesMongo.tvdbHint.getValue());
    seriesPostgres.ignoreTVDB.changeValue(seriesMongo.ignoreTVDB.getValue());
    seriesPostgres.activeEpisodes.changeValue(seriesMongo.activeEpisodes.getValue());
    seriesPostgres.deletedEpisodes.changeValue(seriesMongo.deletedEpisodes.getValue());
    seriesPostgres.suggestionEpisodes.changeValue(seriesMongo.suggestionEpisodes.getValue());
    seriesPostgres.unmatchedEpisodes.changeValue(seriesMongo.unmatchedEpisodes.getValue());
    seriesPostgres.watchedEpisodes.changeValue(seriesMongo.watchedEpisodes.getValue());
    seriesPostgres.unwatchedEpisodes.changeValue(seriesMongo.unwatchedEpisodes.getValue());
    seriesPostgres.unwatchedUnrecorded.changeValue(seriesMongo.unwatchedUnrecorded.getValue());
    seriesPostgres.tvdbOnlyEpisodes.changeValue(seriesMongo.tvdbOnlyEpisodes.getValue());
    seriesPostgres.matchedEpisodes.changeValue(seriesMongo.matchedEpisodes.getValue());
    seriesPostgres.metacritic.changeValue(seriesMongo.metacritic.getValue());
    seriesPostgres.metacriticHint.changeValue(seriesMongo.metacriticHint.getValue());
    seriesPostgres.lastUnwatched.changeValue(seriesMongo.lastUnwatched.getValue());
    seriesPostgres.mostRecent.changeValue(seriesMongo.mostRecent.getValue());
    seriesPostgres.isSuggestion.changeValue(seriesMongo.isSuggestion.getValue());
    seriesPostgres.matchedWrong.changeValue(seriesMongo.matchedWrong.getValue());
    seriesPostgres.needsTVDBRedo.changeValue(seriesMongo.needsTVDBRedo.getValue());
  }

  private SeriesPostgres getOrCreateSeriesPostgresFromTiVoID(String tivoSeriesId) throws SQLException {
    SeriesPostgres seriesPostgres = new SeriesPostgres();
    if (tivoSeriesId == null) {
      seriesPostgres.initializeForInsert();
      return seriesPostgres;
    }

    String sql = "SELECT * FROM series WHERE tivo_series_id = ?";
    ResultSet resultSet = postgresConnection.prepareAndExecuteStatementFetch(sql, tivoSeriesId);

    if (postgresConnection.hasMoreElements(resultSet)) {
      if (devMode) {
        throw new RuntimeException("DEV MODE: Expect to never update. Found series already with existing TiVoSeriesID: " + tivoSeriesId);
      }
      seriesPostgres.initializeFromDBObject(resultSet);
    } else {
      seriesPostgres.initializeForInsert();
    }
    return seriesPostgres;
  }

  private SeriesPostgres getOrCreateSeriesPostgresFromTVDBID(Integer tvdbId) throws SQLException {
    SeriesPostgres seriesPostgres = new SeriesPostgres();
    if (tvdbId == null) {
      seriesPostgres.initializeForInsert();
      return seriesPostgres;
    }

    String sql = "SELECT * FROM series WHERE tvdb_id = ?";
    ResultSet resultSet = postgresConnection.prepareAndExecuteStatementFetch(sql, tvdbId);

    if (postgresConnection.hasMoreElements(resultSet)) {
      if (devMode) {
        throw new RuntimeException("DEV MODE: Expect to never update. Found series already with existing TVDB ID: " + tvdbId);
      }
      seriesPostgres.initializeFromDBObject(resultSet);
    } else {
      seriesPostgres.initializeForInsert();
    }
    return seriesPostgres;
  }

  private EpisodePostgres getOrCreateEpisodePostgres(String tivoProgramId) throws SQLException {
    EpisodePostgres episodePostgres = new EpisodePostgres();
      episodePostgres.initializeForInsert();
      return episodePostgres;
/*
    String sql = "SELECT * FROM episode WHERE tivo_program_id = ? AND retired = ?";
    ResultSet resultSet = postgresConnection.prepareAndExecuteStatementFetch(sql, tivoProgramId, 0);

    if (postgresConnection.hasMoreElements(resultSet)) {
      if (devMode) {
        throw new RuntimeException("DEV MODE: Expect to never update. Found episode already with existing TiVo ID: " + tivoProgramId);
      }
      episodePostgres.initializeFromDBObject(resultSet);
    } else {
      episodePostgres.initializeForInsert();
    }
    return episodePostgres;
    */
  }

  private TiVoEpisodePostgres getOrCreateTiVoEpisodePostgres(String tivoProgramId) throws SQLException {
    TiVoEpisodePostgres tiVoEpisodePostgres = new TiVoEpisodePostgres();
      tiVoEpisodePostgres.initializeForInsert();
      return tiVoEpisodePostgres;

    /*
    String sql = "SELECT * FROM tivo_episode WHERE program_id = ?";
    ResultSet resultSet = postgresConnection.prepareAndExecuteStatementFetch(sql, tivoProgramId);

    if (postgresConnection.hasMoreElements(resultSet)) {
      if (devMode) {
        throw new RuntimeException("DEV MODE: Expect to never update. Found tivo_episode already with existing TiVo ID: " + tivoProgramId);
      }
      tiVoEpisodePostgres.initializeFromDBObject(resultSet);
    } else {
      tiVoEpisodePostgres.initializeForInsert();
    }
    return tiVoEpisodePostgres;
    */
  }

  private TVDBEpisodePostgres getOrCreateTVDBEpisodePostgres(Integer tvdbEpisodeId) throws SQLException {
    TVDBEpisodePostgres tvdbEpisodePostgres = new TVDBEpisodePostgres();
    if (tvdbEpisodeId == null) {
      tvdbEpisodePostgres.initializeForInsert();
      return tvdbEpisodePostgres;
    }

    String sql = "SELECT * FROM tvdb_episode WHERE tvdb_id = ?";
    ResultSet resultSet = postgresConnection.prepareAndExecuteStatementFetch(sql, tvdbEpisodeId);

    if (postgresConnection.hasMoreElements(resultSet)) {
      if (devMode) {
        throw new RuntimeException("DEV MODE: Expect to never update. Found tvdb_episode already with existing TVDB ID: " + tvdbEpisodeId);
      }
      tvdbEpisodePostgres.initializeFromDBObject(resultSet);
    } else {
      tvdbEpisodePostgres.initializeForInsert();
    }
    return tvdbEpisodePostgres;
  }

  private TVDBSeriesPostgres getOrCreateTVDBSeriesPostgres(Integer tvdbId) throws SQLException {
    TVDBSeriesPostgres tvdbSeriesPostgres = new TVDBSeriesPostgres();
    if (tvdbId == null) {
      tvdbSeriesPostgres.initializeForInsert();
      return tvdbSeriesPostgres;
    }

    String sql = "SELECT * FROM tvdb_series WHERE tvdb_id = ?";
    ResultSet resultSet = postgresConnection.prepareAndExecuteStatementFetch(sql, tvdbId);

    if (postgresConnection.hasMoreElements(resultSet)) {
      tvdbSeriesPostgres.initializeFromDBObject(resultSet);
    } else {
      tvdbSeriesPostgres.initializeForInsert();
    }
    return tvdbSeriesPostgres;
  }

  protected void debug(Object object) {
    System.out.println(object);
  }

}
