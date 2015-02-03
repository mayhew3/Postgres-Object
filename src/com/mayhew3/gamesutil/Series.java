package com.mayhew3.gamesutil;

import com.mongodb.DBObject;

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


  public Series(DBObject dbObject) {
    super(dbObject);
  }
}
