package com.mayhew3.gamesutil.mediaobjectmongo;

import org.bson.types.ObjectId;

import java.util.Date;

public class TVDBEpisode extends MediaObject {

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


  public FieldValue<String> tvdbSeriesName = registerStringField("tvdbSeriesName");
  public FieldValue<String> tvdbEpisodeName = registerStringField("tvdbEpisodeName");
  public FieldValue<String> tvdbOverview = registerStringField("tvdbOverview");
  public FieldValue<String> tvdbProductionCode = registerStringField("tvdbProductionCode");
  public FieldValue<String> tvdbDirector = registerStringField("tvdbDirector");
  public FieldValue<String> tvdbWriter = registerStringField("tvdbWriter");
  public FieldValue<String> tvdbFilename = registerStringField("tvdbFilename");

  public FieldValue<Boolean> matchingStump = registerBooleanField("MatchingStump");

  @Override
  protected String getTableName() {
    return "tvdbepisodes";
  }

  @Override
  public String toString() {
      return "";
//    return tvdbSeriesName.getValue() + " " + tvdbEpisodeNumber.getValue() + ": " + tvdbEpisodeName.getValue();
  }
}
