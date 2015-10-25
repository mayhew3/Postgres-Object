package com.mayhew3.gamesutil.mediaobject;

public class SeriesPostgres extends MediaObjectPostgreSQL {

  public FieldValueInteger tier = registerIntegerField("tier");

  public FieldValueString tivoSeriesId = registerStringField("tivo_series_id");
  public FieldValueString seriesTitle = registerStringField("title");
  public FieldValueString tvdbHint = registerStringField("tvdb_hint");

  public FieldValueBoolean ignoreTVDB = registerBooleanField("ignore_tvdb");


  /* Denorms */

  public FieldValueInteger activeEpisodes = registerIntegerField("active_episodes");
  public FieldValueInteger deletedEpisodes = registerIntegerField("deleted_episodes");
  public FieldValueInteger suggestionEpisodes = registerIntegerField("suggestion_episodes");
  public FieldValueInteger unmatchedEpisodes = registerIntegerField("unmatched_episodes");
  public FieldValueInteger watchedEpisodes = registerIntegerField("watched_episodes");
  public FieldValueInteger unwatchedEpisodes = registerIntegerField("unwatched_episodes");
  public FieldValueInteger unwatchedUnrecorded = registerIntegerField("unwatched_unrecorded");
  public FieldValueInteger tvdbOnlyEpisodes = registerIntegerField("tvdb_only_episodes");
  public FieldValueInteger matchedEpisodes = registerIntegerField("matched_episodes");

  public FieldValueInteger metacritic = registerIntegerField("metacritic");
  public FieldValueString metacriticHint = registerStringField("metacritic_hint");

  public FieldValueTimestamp lastUnwatched = registerTimestampField("last_unwatched");
  public FieldValueTimestamp mostRecent = registerTimestampField("most_recent");
  public FieldValueBoolean isSuggestion = registerBooleanField("suggestion");

  public FieldValueBoolean matchedWrong = registerBooleanField("matched_wrong");

  public FieldValueBoolean needsTVDBRedo = registerBooleanField("needs_tvdb_redo");

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
}
