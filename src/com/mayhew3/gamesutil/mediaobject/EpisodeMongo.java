package com.mayhew3.gamesutil.mediaobject;

import org.bson.types.ObjectId;

import java.util.Date;

public class EpisodeMongo extends MediaObjectMongoDB {


  public FieldValue<Date> watchedDate = registerDateField("WatchedDate");

  public FieldValueDate tivoShowingStartTime = registerDateField("TiVoShowingStartTime");
  public FieldValue<Date> tivoDeletedDate = registerDateField("TiVoDeletedDate");
  public FieldValueDate tivoCaptureDate = registerDateField("TiVoCaptureDate");

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


  public FieldValue<Integer> tivoEpisodeNumber = registerIntegerField("TiVoEpisodeNumber");
  public FieldValue<Integer> tivoDuration = registerIntegerField("TiVoDuration");
  public FieldValue<Integer> tivoShowingDuration = registerIntegerField("TiVoShowingDuration");
  public FieldValue<Integer> tivoChannel = registerIntegerField("TiVoChannel");
  public FieldValue<Integer> tivoRating = registerIntegerField("TiVoRating");

  public FieldValue<String> tivoSeriesId = registerStringField("TiVoSeriesId");
  public FieldValue<String> tivoProgramId = registerStringField("TiVoProgramId");
  public FieldValue<String> tivoSeriesTitle = registerStringField("TiVoSeriesTitle");
  public FieldValue<String> tivoEpisodeTitle = registerStringField("TiVoEpisodeTitle");
  public FieldValue<String> tivoDescription = registerStringField("TiVoDescription");
  public FieldValue<String> tivoStation = registerStringField("TiVoStation");
  public FieldValue<String> tivoUrl = registerStringField("TiVoUrl");

  public FieldValue<String> tvdbSeriesName = registerStringField("tvdbSeriesName");
  public FieldValue<String> tvdbEpisodeName = registerStringField("tvdbEpisodeName");
  public FieldValue<String> tvdbOverview = registerStringField("tvdbOverview");
  public FieldValue<String> tvdbProductionCode = registerStringField("tvdbProductionCode");
  public FieldValue<String> tvdbDirector = registerStringField("tvdbDirector");
  public FieldValue<String> tvdbWriter = registerStringField("tvdbWriter");
  public FieldValue<String> tvdbFilename = registerStringField("tvdbFilename");

  public FieldValue<Boolean> onTiVo = registerBooleanField("OnTiVo");
  public FieldValue<Boolean> tivoHD = registerBooleanField("TiVoHD");
  public FieldValue<Boolean> watched = registerBooleanField("Watched");
  public FieldValue<Boolean> tivoSuggestion = registerBooleanField("TiVoSuggestion");
  public FieldValue<Boolean> matchingStump = registerBooleanField("MatchingStump");
  public FieldValue<Boolean> hasDuplicates = registerBooleanField("HasDuplicates");

  @Override
  protected String getTableName() {
    return "episodes";
  }

  @Override
  public String toString() {
    String seriesTitle = tivoSeriesTitle.getValue() == null ? tvdbSeriesName.getValue() : tivoSeriesTitle.getValue();
    String episodeTitle = tivoEpisodeTitle.getValue() == null ? tvdbEpisodeName.getValue() : tivoEpisodeTitle.getValue();
    Integer episodeNumber = tivoEpisodeNumber.getValue() == null ? tvdbEpisodeNumber.getValue() : tivoEpisodeNumber.getValue();
    String seasonNumber = tvdbSeason.getValue() == null ? "" : (tvdbSeason.getValue() + "x");
    return seriesTitle + " " + seasonNumber + episodeNumber + ": " + episodeTitle;
  }
}
