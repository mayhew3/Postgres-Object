package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.games.MongoConnection;
import com.mayhew3.gamesutil.games.PostgresConnection;
import com.mayhew3.gamesutil.mediaobject.SeriesMongo;
import com.mayhew3.gamesutil.mediaobject.SeriesPostgres;
import com.mayhew3.gamesutil.mediaobject.TVDBSeriesPostgres;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TVPostgresMigration {
  private static MongoConnection mongoConnection;
  private static PostgresConnection postgresConnection;

  public static void main(String[] args) throws SQLException {
    postgresConnection = new PostgresConnection();
    mongoConnection = new MongoConnection("tv");

    TVPostgresMigration tvPostgresMigration = new TVPostgresMigration();
    tvPostgresMigration.updatePostgresDatabase();
  }

  public void updatePostgresDatabase() throws SQLException {
    DBObject dbObject = new BasicDBObject()
        .append("IsEpisodic", true)
//        .append("IsSuggestion", new BasicDBObject("$ne", true))

        // todo: figure out what series don't have TiVoSeriesId
        .append("TiVoSeriesId", new BasicDBObject("$ne", null))
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

    SeriesPostgres seriesPostgres = getOrCreateSeriesPostgres(seriesMongo.tivoSeriesId.getValue());

    String title = seriesMongo.seriesTitle.getValue();

    if (seriesPostgres.id.getValue() != null) {
      debug(title + ": Updating");
    } else {
      debug(title + ": Inserting");
    }

    seriesPostgres.tier.changeValue(seriesMongo.tier.getValue());
    seriesPostgres.tivoSeriesId.changeValue(seriesMongo.tivoSeriesId.getValue());
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

    seriesPostgres.commit(postgresConnection);

    if (seriesMongo.tvdbId.getValue() != null) {
      debug("   (also copying tvdb info...)");

      TVDBSeriesPostgres tvdbSeriesPostgres = getOrCreateTVDBSeriesPostgres(seriesMongo.tvdbId.getValue());

      tvdbSeriesPostgres.seriesId.changeValue(seriesPostgres.id.getValue());

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

      tvdbSeriesPostgres.commit(postgresConnection);
    }



  }

  private SeriesPostgres getOrCreateSeriesPostgres(String tivoSeriesId) throws SQLException {
    SeriesPostgres seriesPostgres = new SeriesPostgres();
    if (tivoSeriesId == null) {
      seriesPostgres.initializeForInsert();
      return seriesPostgres;
    }
    String sql = "SELECT * FROM series WHERE tivo_series_id = ?";
    ResultSet resultSet = postgresConnection.prepareAndExecuteStatementFetch(sql, tivoSeriesId);


    if (postgresConnection.hasMoreElements(resultSet)) {
      seriesPostgres.initializeFromDBObject(resultSet);
    } else {
      seriesPostgres.initializeForInsert();
    }
    return seriesPostgres;
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
