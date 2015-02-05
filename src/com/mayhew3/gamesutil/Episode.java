package com.mayhew3.gamesutil;

import java.util.Date;

public class Episode extends MediaObject {

  FieldValue<Date> dateAdded = registerDateField("DateAdded");
  FieldValue<Date> watchedDate = registerDateField("WatchedDate");

  FieldValue<Date> tiVoShowingStartTime = registerDateField("TiVoShowingStartTime");
  FieldValue<Date> tiVoDeletedDate = registerDateField("TiVoDeletedDate");
  FieldValue<Date> tiVoCaptureDate = registerDateField("TiVoCaptureDate");

  FieldValue<Date> tvdbFirstAired = registerDateField("tvdbFirstAired");

  FieldValue<Integer> tvdbSeason = registerIntegerField("tvdbSeason");
  FieldValue<Integer> tvdbSeasonId = registerIntegerField("tvdbSeasonId");
  FieldValue<Integer> tvdbEpisodeId = registerIntegerField("tvdbEpisodeId");
  FieldValue<Integer> tvdbEpisodeNumber = registerIntegerField("tvdbEpisodeNumber");
  FieldValue<Integer> tiVoEpisodeNumber = registerIntegerField("TiVoEpisodeNumber");

  FieldValue<String> tivoSeriesTitle = registerStringField("TiVoSeriesTitle");
  FieldValue<String> tivoEpisodeTitle = registerStringField("TiVoEpisodeTitle");

  FieldValue<String> tvdbSeriesName = registerStringField("tvdbSeriesName");
  FieldValue<String> tvdbEpisodeName = registerStringField("tvdbEpisodeName");

  FieldValue<Boolean> onTiVo = registerBooleanField("OnTiVo");
  FieldValue<Boolean> watched = registerBooleanField("Watched");
  FieldValue<Boolean> tiVoSuggestion = registerBooleanField("TiVoSuggestion");
  FieldValue<Boolean> matchedStump = registerBooleanField("MatchedStump");

  @Override
  protected String getTableName() {
    return "episodes";
  }

  @Override
  public String toString() {
    String seriesTitle = tivoSeriesTitle.getValue() == null ? tvdbSeriesName.getValue() : tivoSeriesTitle.getValue();
    String episodeTitle = tivoEpisodeTitle.getValue() == null ? tvdbEpisodeName.getValue() : tivoEpisodeTitle.getValue();
    Integer episodeNumber = tiVoEpisodeNumber.getValue() == null ? tvdbEpisodeNumber.getValue() : tiVoEpisodeNumber.getValue();
    return seriesTitle + " " + episodeNumber + ": " + episodeTitle;
  }
}
