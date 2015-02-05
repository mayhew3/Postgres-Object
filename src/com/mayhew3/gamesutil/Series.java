package com.mayhew3.gamesutil;

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

  FieldValue<Integer> tvdbId = registerIntegerField("tvdbId");
  FieldValue<Integer> tvdbRatingCount = registerIntegerField("tvdbRatingCount");
  FieldValue<Integer> tvdbRuntime = registerIntegerField("tvdbRuntime");

  FieldValue<String> seriesTitle = registerStringField("SeriesTitle");

  @Override
  protected String getTableName() {
    return "series";
  }

  @Override
  public String toString() {
    return seriesTitle.getValue();
  }
}
