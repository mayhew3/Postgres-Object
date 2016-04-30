package com.mayhew3.gamesutil.dataobject;

import com.google.common.base.Preconditions;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Series extends DataObject {

  /* Foreign Keys */
  public FieldValueInteger tvdbSeriesId = registerIntegerField("tvdb_series_id", Nullability.NULLABLE);

  /* Data */
  public FieldValueString seriesTitle = registerStringField("title", Nullability.NULLABLE);
  public FieldValueInteger tier = registerIntegerField("tier", Nullability.NULLABLE);
  public FieldValueInteger metacritic = registerIntegerField("metacritic", Nullability.NULLABLE);
  public FieldValueInteger my_rating = registerIntegerField("my_rating", Nullability.NULLABLE);
  public FieldValueString tivoSeriesId = registerStringField("tivo_series_id", Nullability.NULLABLE);
  public FieldValueInteger tvdbId = registerIntegerField("tvdb_id", Nullability.NULLABLE);

  /* Matching Helpers */
  public FieldValueString metacriticHint = registerStringField("metacritic_hint", Nullability.NULLABLE);
  public FieldValueBoolean ignoreTVDB = registerBooleanField("ignore_tvdb", Nullability.NULLABLE);
  public FieldValueBoolean matchedWrong = registerBooleanField("matched_wrong", Nullability.NOT_NULL);
  public FieldValueBoolean needsTVDBRedo = registerBooleanField("needs_tvdb_redo", Nullability.NOT_NULL);
  public FieldValueString tvdbHint = registerStringField("tvdb_hint", Nullability.NULLABLE);
  public FieldValueString tivoName = registerStringField("tivo_name", Nullability.NULLABLE);


  /* Denorms */
  public FieldValueInteger activeEpisodes = registerIntegerField("active_episodes", Nullability.NOT_NULL);
  public FieldValueInteger deletedEpisodes = registerIntegerField("deleted_episodes", Nullability.NOT_NULL);
  public FieldValueInteger suggestionEpisodes = registerIntegerField("suggestion_episodes", Nullability.NOT_NULL);
  public FieldValueInteger unmatchedEpisodes = registerIntegerField("unmatched_episodes", Nullability.NOT_NULL);
  public FieldValueInteger watchedEpisodes = registerIntegerField("watched_episodes", Nullability.NOT_NULL);
  public FieldValueInteger unwatchedEpisodes = registerIntegerField("unwatched_episodes", Nullability.NOT_NULL);
  public FieldValueInteger unwatchedUnrecorded = registerIntegerField("unwatched_unrecorded", Nullability.NOT_NULL);
  public FieldValueInteger tvdbOnlyEpisodes = registerIntegerField("tvdb_only_episodes", Nullability.NOT_NULL);
  public FieldValueInteger matchedEpisodes = registerIntegerField("matched_episodes", Nullability.NOT_NULL);
  public FieldValueInteger streamingEpisodes = registerIntegerField("streaming_episodes", Nullability.NOT_NULL);
  public FieldValueInteger unwatchedStreaming = registerIntegerField("unwatched_streaming", Nullability.NOT_NULL);

  public FieldValueTimestamp lastUnwatched = registerTimestampField("last_unwatched", Nullability.NULLABLE);
  public FieldValueTimestamp mostRecent = registerTimestampField("most_recent", Nullability.NULLABLE);
  public FieldValueBoolean isSuggestion = registerBooleanField("suggestion", Nullability.NOT_NULL);

  @Override
  protected String getTableName() {
    return "series";
  }

  @Override
  public String toString() {
    return seriesTitle.getValue();
  }

  public void initializeDenorms() {
    activeEpisodes.changeValue(0);
    deletedEpisodes.changeValue(0);
    suggestionEpisodes.changeValue(0);
    unmatchedEpisodes.changeValue(0);
    watchedEpisodes.changeValue(0);
    unwatchedEpisodes.changeValue(0);
    unwatchedUnrecorded.changeValue(0);
    tvdbOnlyEpisodes.changeValue(0);
    matchedEpisodes.changeValue(0);

    ignoreTVDB.changeValue(false);
    isSuggestion.changeValue(false);
    needsTVDBRedo.changeValue(false);
    matchedWrong.changeValue(false);
  }

  /**
   * @param connection DB connection to use
   * @param genreName Name of new or existing genre
   * @return New SeriesGenrePostgres join entity, if a new one was created. Null otherwise.
   * @throws SQLException
   */
  @Nullable
  public SeriesGenre addGenre(SQLConnection connection, String genreName) throws SQLException {
    Preconditions.checkNotNull(id.getValue(), "Cannot insert join entity until Series object is committed (id is non-null)");

    Genre genre = Genre.findOrCreate(connection, genreName);

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * FROM series_genre WHERE series_id = ? AND genre_id = ?",
        id.getValue(),
        genre.id.getValue());

    if (!resultSet.next()) {
      SeriesGenre seriesGenre = new SeriesGenre();
      seriesGenre.initializeForInsert();

      seriesGenre.seriesId.changeValue(id.getValue());
      seriesGenre.genreId.changeValue(genre.id.getValue());

      seriesGenre.commit(connection);
      return seriesGenre;
    }

    return null;
  }

  public PossibleSeriesMatch addPossibleSeriesMatch(SQLConnection connection, Integer tvdbSeriesId, String title) throws SQLException {
    Preconditions.checkNotNull(id.getValue(), "Cannot insert join entity until Series object is committed (id is non-null)");

    PossibleSeriesMatch matchPostgres = new PossibleSeriesMatch();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * FROM possible_series_match " +
            "WHERE " + matchPostgres.tvdbSeriesId.getFieldName() + " = ?",
        tvdbSeriesId);

    if (resultSet.next()) {
      matchPostgres.initializeFromDBObject(resultSet);
    } else {
      matchPostgres.initializeForInsert();
      matchPostgres.seriesId.changeValue(id.getValue());
      matchPostgres.tvdbSeriesId.changeValue(tvdbSeriesId);
      matchPostgres.tvdbSeriesTitle.changeValue(title);
      matchPostgres.commit(connection);
    }

    return matchPostgres;
  }

  /**
   * @param connection DB connection to use
   * @param viewingLocationName Name of new or existing viewing location
   * @return New {{@link}SeriesViewingLocationPostgres} join entity, if a new one was created. Null otherwise.
   * @throws SQLException
   */
  @Nullable
  public SeriesViewingLocation addViewingLocation(SQLConnection connection, String viewingLocationName) throws SQLException {
    Preconditions.checkNotNull(id.getValue(), "Cannot insert join entity until Series object is committed (id is non-null)");

    ViewingLocation viewingLocation = ViewingLocation.findOrCreate(connection, viewingLocationName);

    SeriesViewingLocation seriesViewingLocation = new SeriesViewingLocation();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * FROM " + seriesViewingLocation.getTableName() + " " +
            "WHERE " + seriesViewingLocation.seriesId.getFieldName() + " = ? " +
            "AND " + seriesViewingLocation.viewingLocationId.getFieldName() + " = ?",
        id.getValue(),
        viewingLocation.id.getValue());

    if (!resultSet.next()) {
      seriesViewingLocation.initializeForInsert();

      seriesViewingLocation.seriesId.changeValue(id.getValue());
      seriesViewingLocation.viewingLocationId.changeValue(viewingLocation.id.getValue());

      seriesViewingLocation.commit(connection);
      return seriesViewingLocation;
    }

    return null;
  }

  private List<ViewingLocation> getViewingLocations(SQLConnection connection) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT vl.* " +
            "FROM viewing_location vl " +
            "INNER JOIN series_viewing_location svl " +
            " ON svl.viewing_location_id = vl.id " +
            "WHERE svl.series_id = ?",
        id.getValue()
    );

    List<ViewingLocation> viewingLocations = new ArrayList<>();
    while (resultSet.next()) {
      ViewingLocation viewingLocation = new ViewingLocation();
      viewingLocation.initializeFromDBObject(resultSet);
      viewingLocations.add(viewingLocation);
    }
    return viewingLocations;
  }

  public Boolean isStreaming(SQLConnection connection) throws SQLException {
    for (ViewingLocation viewingLocation : getViewingLocations(connection)) {
      if (viewingLocation.streaming.getValue()) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  public Season getOrCreateSeason(SQLConnection connection, Integer seasonNumber) throws SQLException {
    Preconditions.checkNotNull(id.getValue(), "Cannot insert join entity until Series object is committed (id is non-null)");

    Season season = new Season();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * FROM " + season.getTableName() + " " +
            "WHERE " + season.seriesId.getFieldName() + " = ? " +
            "AND " + season.seasonNumber.getFieldName() + " = ?",
        id.getValue(),
        seasonNumber);
    if (resultSet.next()) {
      season.initializeFromDBObject(resultSet);
    } else {
      season.initializeForInsert();
      season.seriesId.changeValue(id.getValue());
      season.seasonNumber.changeValue(seasonNumber);

      season.commit(connection);
    }

    return season;
  }

  @NotNull
  public List<Episode> getEpisodes(SQLConnection connection) throws SQLException {
    List<Episode> episodes = new ArrayList<>();
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT e.* " +
            "FROM episode e " +
            "WHERE e.seriesid = ? " +
            "AND e.retired = ?", id.getValue(), 0);

    while (resultSet.next()) {
      Episode episode = new Episode();
      episode.initializeFromDBObject(resultSet);
      episodes.add(episode);
    }
    return episodes;
  }

  public Optional<TVDBSeries> getTVDBSeries(SQLConnection connection) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM tvdb_series " +
            "WHERE id = ? " +
            "AND retired = ?", tvdbSeriesId.getValue(), 0
    );

    if (resultSet.next()) {
      TVDBSeries tvdbSeries = new TVDBSeries();
      tvdbSeries.initializeFromDBObject(resultSet);
      return Optional.of(tvdbSeries);
    } else {
      return Optional.empty();
    }
  }
}
