package com.mayhew3.gamesutil.mediaobject;

public class TVDBEpisodePostgres extends MediaObjectPostgreSQL {

  public FieldValue<Integer> episodeId = registerIntegerField("episodeid");

  public FieldValue<Integer> seasonNumber = registerIntegerField("season_number");
  public FieldValue<Integer> seasonId = registerIntegerField("season_id");
  public FieldValue<Integer> tvdbId = registerIntegerField("tvdb_id");
  public FieldValue<Integer> episodeNumber = registerIntegerField("episode_number");
  public FieldValue<Integer> absoluteNumber = registerIntegerField("absolute_number");
  public FieldValue<Integer> ratingCount = registerIntegerField("rating_count");
  public FieldValue<Integer> airsAfterSeason = registerIntegerField("airs_after_season");
  public FieldValue<Integer> airsBeforeSeason = registerIntegerField("airs_before_season");
  public FieldValue<Integer> airsBeforeEpisode = registerIntegerField("airs_before_episode");
  public FieldValue<Integer> thumbHeight = registerIntegerField("thumb_height");
  public FieldValue<Integer> thumbWidth = registerIntegerField("thumb_width");


  public FieldValueTimestamp firstAired = registerTimestampField("first_aired");
  public FieldValue<Integer> lastUpdated = registerIntegerField("last_updated");

  public FieldValueBigDecimal rating = registerBigDecimalField("rating");


  public FieldValue<String> seriesName = registerStringField("series_name");
  public FieldValue<String> name = registerStringField("name");
  public FieldValue<String> overview = registerStringField("overview");
  public FieldValue<String> productionCode = registerStringField("production_code");
  public FieldValue<String> director = registerStringField("director");
  public FieldValue<String> writer = registerStringField("writer");
  public FieldValue<String> filename = registerStringField("filename");


  @Override
  protected String getTableName() {
    return "tvdb_episode";
  }

  @Override
  public String toString() {
    return seriesName.getValue() + " " + episodeNumber.getValue() + ": " + name.getValue();
  }
}
