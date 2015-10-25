package com.mayhew3.gamesutil.mediaobject;

import java.util.Date;

public class EpisodePostgres extends MediaObjectPostgreSQL {


  public FieldValue<Date> watchedDate = registerDateField("watched_date");

  public FieldValue<Integer> season = registerIntegerField("season");
  public FieldValue<Integer> seasonEpisodeNumber = registerIntegerField("season_episode_number");
  public FieldValue<Integer> episodeNumber = registerIntegerField("episode_number");

  public FieldValue<Date> airDate = registerDateField("air_date");

  public FieldValueInteger seriesId = registerIntegerField("seriesid");

  public FieldValue<Boolean> onTiVo = registerBooleanField("on_tivo");
  public FieldValue<Boolean> watched = registerBooleanField("watched");

  public FieldValueString title = registerStringField("title");
  public FieldValueString seriesTitle = registerStringField("series_title");


  @Override
  protected String getTableName() {
    return "episode";
  }

  @Override
  public String toString() {
    return seriesTitle.getValue() + " " + episodeNumber.getValue() + ": " + title.getValue();
  }
}
