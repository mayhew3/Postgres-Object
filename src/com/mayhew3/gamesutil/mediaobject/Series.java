package com.mayhew3.gamesutil.mediaobject;

import com.mongodb.BasicDBList;

import java.util.Date;

public class Series extends MediaObject {

  public FieldValueInteger activeEpisodes = registerIntegerField("ActiveEpisodes");
  public FieldValueInteger deletedEpisodes = registerIntegerField("DeletedEpisodes");
  public FieldValueInteger suggestionEpisodes = registerIntegerField("SuggestionEpisodes");
  public FieldValueInteger unmatchedEpisodes = registerIntegerField("UnmatchedEpisodes");
  public FieldValueInteger watchedEpisodes = registerIntegerField("WatchedEpisodes");
  public FieldValueInteger unwatchedEpisodes = registerIntegerField("UnwatchedEpisodes");
  public FieldValueInteger unwatchedUnrecorded = registerIntegerField("UnwatchedUnrecorded");
  public FieldValueInteger tvdbOnlyEpisodes = registerIntegerField("tvdbOnlyEpisodes");
  public FieldValueInteger matchedEpisodes = registerIntegerField("MatchedEpisodes");

  public FieldValue<Date> lastUnwatched = registerDateField("LastUnwatched");
  public FieldValue<Date> mostRecent = registerDateField("MostRecent");
  public FieldValue<Date> tvdbFirstAired = registerDateField("tvdbFirstAired");

  public FieldValue<String> seriesId = registerStringField("SeriesId");
  public FieldValueInteger tvdbId = registerIntegerField("tvdbId");
  public FieldValue<String> tvdbSeriesId = registerStringField("tvdbSeriesId");
  public FieldValueInteger tvdbRatingCount = registerIntegerField("tvdbRatingCount");
  public FieldValueInteger tvdbRuntime = registerIntegerField("tvdbRuntime");

  public FieldValue<Double> tvdbRating = registerDoubleField("tvdbRating");

  public FieldValueMongoArray episodes = registerMongoArrayField("episodes");

  public FieldValue<String> seriesTitle = registerStringField("SeriesTitle");
  public FieldValue<String> tivoName = registerStringField("TiVoName");
  public FieldValue<String> tvdbHint = registerStringField("TVDBHint");
  public FieldValue<String> tvdbName = registerStringField("tvdbName");
  public FieldValue<String> tvdbAirsDayOfWeek = registerStringField("tvdbAirsDayOfWeek");
  public FieldValue<String> tvdbAirsTime = registerStringField("tvdbAirsTime");
  public FieldValue<String> tvdbNetwork = registerStringField("tvdbNetwork");
  public FieldValue<String> tvdbOverview = registerStringField("tvdbOverview");
  public FieldValue<String> tvdbStatus = registerStringField("tvdbStatus");
  public FieldValue<String> tvdbPoster = registerStringField("tvdbPoster");
  public FieldValue<String> tvdbBanner = registerStringField("tvdbBanner");
  public FieldValue<String> tvdbLastUpdated = registerStringField("tvdbLastUpdated");
  public FieldValue<String> imdbId = registerStringField("imdbId");
  public FieldValue<String> zap2it_id = registerStringField("zap2it_id");

  public FieldValue<BasicDBList> tvdbGenre = registerStringArrayField("tvdbGenre");

  public FieldValue<Boolean> ignoreTVDB = registerBooleanField("IgnoreTVDB");



  @Override
  protected String getTableName() {
    return "series";
  }

  @Override
  public String toString() {
    return seriesTitle.getValue();
  }
}
