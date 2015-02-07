package com.mayhew3.gamesutil;

import com.mongodb.BasicDBList;

import java.util.Date;

public class Series extends MediaObject {

  FieldValue<Integer> activeEpisodes = registerIntegerField("ActiveEpisodes");
  FieldValue<Integer> deletedEpisodes = registerIntegerField("DeletedEpisodes");
  FieldValue<Integer> suggestionEpisodes = registerIntegerField("SuggestionEpisodes");
  FieldValue<Integer> unmatchedEpisodes = registerIntegerField("UnmatchedEpisodes");
  FieldValue<Integer> watchedEpisodes = registerIntegerField("WatchedEpisodes");
  FieldValue<Integer> unwatchedEpisodes = registerIntegerField("UnwatchedEpisodes");
  FieldValue<Integer> unwatchedUnrecorded = registerIntegerField("UnwatchedUnrecorded");
  FieldValue<Integer> tvdbOnlyEpisodes = registerIntegerField("tvdbOnlyEpisodes");
  FieldValue<Integer> matchedEpisodes = registerIntegerField("MatchedEpisodes");

  FieldValue<Date> lastUnwatched = registerDateField("LastUnwatched");
  FieldValue<Date> mostRecent = registerDateField("MostRecent");
  FieldValue<Date> tvdbFirstAired = registerDateField("tvdbFirstAired");

  FieldValue<String> seriesId = registerStringField("SeriesId");
  FieldValue<Integer> tvdbId = registerIntegerField("tvdbId");
  FieldValue<Integer> tvdbSeriesId = registerIntegerField("tvdbSeriesId");
  FieldValue<Integer> tvdbRatingCount = registerIntegerField("tvdbRatingCount");
  FieldValue<Integer> tvdbRuntime = registerIntegerField("tvdbRuntime");

  FieldValue<Double> tvdbRating = registerDoubleField("tvdbRating");

  FieldValueMongoArray episodes = registerMongoArrayField("episodes");

  FieldValue<String> seriesTitle = registerStringField("SeriesTitle");
  FieldValue<String> tivoName = registerStringField("TiVoName");
  FieldValue<String> tvdbHint = registerStringField("TVDBHint");
  FieldValue<String> tvdbName = registerStringField("tvdbName");
  FieldValue<String> tvdbAirsDayOfWeek = registerStringField("tvdbAirsDayOfWeek");
  FieldValue<String> tvdbAirsTime = registerStringField("tvdbAirsTime");
  FieldValue<String> tvdbNetwork = registerStringField("tvdbNetwork");
  FieldValue<String> tvdbOverview = registerStringField("tvdbOverview");
  FieldValue<String> tvdbStatus = registerStringField("tvdbStatus");
  FieldValue<String> tvdbPoster = registerStringField("tvdbPoster");
  FieldValue<String> tvdbBanner = registerStringField("tvdbBanner");
  FieldValue<String> tvdbLastUpdated = registerStringField("tvdbLastUpdated");
  FieldValue<String> imdbId = registerStringField("imdbId");
  FieldValue<String> zap2it_id = registerStringField("zap2it_id");

  FieldValue<BasicDBList> tvdbGenre = registerStringArrayField("tvdbGenre");

  FieldValue<Boolean> ignoreTVDB = registerBooleanField("IgnoreTVDB");



  @Override
  protected String getTableName() {
    return "series";
  }

  @Override
  public String toString() {
    return seriesTitle.getValue();
  }
}
