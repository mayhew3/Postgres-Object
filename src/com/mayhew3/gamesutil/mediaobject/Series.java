package com.mayhew3.gamesutil.mediaobject;

import com.mongodb.BasicDBList;

import java.util.Date;

public class Series extends MediaObject {

  public FieldValue<Integer> activeEpisodes = registerIntegerField("ActiveEpisodes");
  public FieldValue<Integer> deletedEpisodes = registerIntegerField("DeletedEpisodes");
  public FieldValue<Integer> suggestionEpisodes = registerIntegerField("SuggestionEpisodes");
  public FieldValue<Integer> unmatchedEpisodes = registerIntegerField("UnmatchedEpisodes");
  public FieldValue<Integer> watchedEpisodes = registerIntegerField("WatchedEpisodes");
  public FieldValue<Integer> unwatchedEpisodes = registerIntegerField("UnwatchedEpisodes");
  public FieldValue<Integer> unwatchedUnrecorded = registerIntegerField("UnwatchedUnrecorded");
  public FieldValue<Integer> tvdbOnlyEpisodes = registerIntegerField("tvdbOnlyEpisodes");
  public FieldValue<Integer> matchedEpisodes = registerIntegerField("MatchedEpisodes");

  public FieldValue<Date> lastUnwatched = registerDateField("LastUnwatched");
  public FieldValue<Date> mostRecent = registerDateField("MostRecent");
  public FieldValue<Date> tvdbFirstAired = registerDateField("tvdbFirstAired");

  public FieldValue<String> seriesId = registerStringField("SeriesId");
  public FieldValue<Integer> tvdbId = registerIntegerField("tvdbId");
  public FieldValue<String> tvdbSeriesId = registerStringField("tvdbSeriesId");
  public FieldValue<Integer> tvdbRatingCount = registerIntegerField("tvdbRatingCount");
  public FieldValue<Integer> tvdbRuntime = registerIntegerField("tvdbRuntime");

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
