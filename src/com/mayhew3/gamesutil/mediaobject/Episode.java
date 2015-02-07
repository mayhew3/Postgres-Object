package com.mayhew3.gamesutil.mediaobject;

import org.bson.types.ObjectId;

import java.util.Date;

public class Episode extends MediaObject {


  public FieldValue<Date> watchedDate = registerDateField("WatchedDate");

  public FieldValue<Date> tiVoShowingStartTime = registerDateField("TiVoShowingStartTime");
  public FieldValue<Date> tiVoDeletedDate = registerDateField("TiVoDeletedDate");
  public FieldValue<Date> tiVoCaptureDate = registerDateField("TiVoCaptureDate");

  public FieldValue<Date> tvdbFirstAired = registerDateField("tvdbFirstAired");
  public FieldValue<Integer> tvdbLastUpdated = registerIntegerField("tvdbLastUpdated");

  public FieldValue<ObjectId> seriesId = registerObjectIdField("SeriesId");

  public FieldValue<Integer> tvdbSeason = registerIntegerField("tvdbSeason");
  public FieldValue<Integer> tvdbSeasonId = registerIntegerField("tvdbSeasonId");
  public FieldValue<Integer> tvdbEpisodeId = registerIntegerField("tvdbEpisodeId");
  public FieldValue<Integer> tvdbEpisodeNumber = registerIntegerField("tvdbEpisodeNumber");
  public FieldValue<Integer> tvdbAbsoluteNumber = registerIntegerField("tvdbAbsoluteNumber");
  public FieldValue<Integer> tvdbRatingCount = registerIntegerField("tvdbRatingCount");
  public FieldValue<Integer> tvdbAirsAfterSeason = registerIntegerField("tvdbAirsAfterSeason");
  public FieldValue<Integer> tvdbAirsBeforeSeason = registerIntegerField("tvdbAirsBeforeSeason");
  public FieldValue<Integer> tvdbAirsBeforeEpisode = registerIntegerField("tvdbAirsBeforeEpisode");
  public FieldValue<Integer> tvdbThumbHeight = registerIntegerField("tvdbThumbHeight");
  public FieldValue<Integer> tvdbThumbWidth = registerIntegerField("tvdbThumbWidth");

  public FieldValue<Double> tvdbRating = registerDoubleField("tvdbRating");


  public FieldValue<Integer> tiVoEpisodeNumber = registerIntegerField("TiVoEpisodeNumber");

  public FieldValue<String> tivoSeriesId = registerStringField("TiVoSeriesId");
  public FieldValue<String> tivoSeriesTitle = registerStringField("TiVoSeriesTitle");
  public FieldValue<String> tivoEpisodeTitle = registerStringField("TiVoEpisodeTitle");

  public FieldValue<String> tvdbSeriesName = registerStringField("tvdbSeriesName");
  public FieldValue<String> tvdbEpisodeName = registerStringField("tvdbEpisodeName");
  public FieldValue<String> tvdbOverview = registerStringField("tvdbOverview");
  public FieldValue<String> tvdbProductionCode = registerStringField("tvdbProductionCode");
  public FieldValue<String> tvdbDirector = registerStringField("tvdbDirector");
  public FieldValue<String> tvdbWriter = registerStringField("tvdbWriter");
  public FieldValue<String> tvdbFilename = registerStringField("tvdbFilename");

  public FieldValue<Boolean> onTiVo = registerBooleanField("OnTiVo");
  public FieldValue<Boolean> watched = registerBooleanField("Watched");
  public FieldValue<Boolean> tiVoSuggestion = registerBooleanField("TiVoSuggestion");
  public FieldValue<Boolean> matchedStump = registerBooleanField("MatchedStump");

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
