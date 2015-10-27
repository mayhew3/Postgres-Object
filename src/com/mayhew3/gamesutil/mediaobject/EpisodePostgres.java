package com.mayhew3.gamesutil.mediaobject;

public class EpisodePostgres extends MediaObjectPostgreSQL {

  /* Foreign Keys */
  public FieldValue<Integer> tivoEpisodeId = registerIntegerField("tivo_episode_id");
  public FieldValue<Integer> tvdbEpisodeId = registerIntegerField("tvdb_episode_id");

  /* Data */
  public FieldValueTimestamp watchedDate = registerTimestampField("watched_date");

  public FieldValue<Integer> season = registerIntegerField("season");
  public FieldValue<Integer> seasonEpisodeNumber = registerIntegerField("season_episode_number");
  public FieldValue<Integer> episodeNumber = registerIntegerField("episode_number");

  public FieldValueTimestamp airDate = registerTimestampField("air_date");

  public FieldValueInteger seriesId = registerIntegerField("seriesid");

  public FieldValue<Boolean> onTiVo = registerBooleanField("on_tivo");
  public FieldValue<Boolean> watched = registerBooleanField("watched");

  public FieldValueString title = registerStringField("title");
  public FieldValueString seriesTitle = registerStringField("series_title");

  public FieldValueString tivoProgramId = registerStringField("tivo_program_id");




  @Override
  protected String getTableName() {
    return "episode";
  }

  @Override
  public String toString() {
    return seriesTitle.getValue() + " " + episodeNumber.getValue() + ": " + title.getValue();
  }
}
