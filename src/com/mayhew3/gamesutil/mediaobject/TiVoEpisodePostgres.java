package com.mayhew3.gamesutil.mediaobject;

import java.util.Date;

public class TiVoEpisodePostgres extends MediaObjectPostgreSQL {


  public FieldValueInteger episodeId = registerIntegerField("episodeid");

  public FieldValue<Boolean> suggestion = registerBooleanField("suggestion");

  public FieldValueString title = registerStringField("title");

  public FieldValueDate showingStartTime = registerDateField("showing_start_time");
  public FieldValue<Date> deletedDate = registerDateField("deleted_date");
  public FieldValueDate captureDate = registerDateField("capture_date");

  public FieldValue<Boolean> hd = registerBooleanField("hd");

  public FieldValue<Integer> episodeNumber = registerIntegerField("episode_number");
  public FieldValue<Integer> duration = registerIntegerField("duration");
  public FieldValue<Integer> showingDuration = registerIntegerField("showing_duration");
  public FieldValue<Integer> channel = registerIntegerField("channel");
  public FieldValue<Integer> rating = registerIntegerField("rating");

  public FieldValue<String> tivoSeriesId = registerStringField("tivo_series_id");
  public FieldValue<String> programId = registerStringField("program_id");
  public FieldValue<String> seriesTitle = registerStringField("series_title");
  public FieldValue<String> description = registerStringField("description");
  public FieldValue<String> station = registerStringField("station");
  public FieldValue<String> url = registerStringField("url");


  @Override
  protected String getTableName() {
    return "tivo_episode";
  }

  @Override
  public String toString() {
    return seriesTitle.getValue() + " " + episodeNumber.getValue() + ": " + title.getValue();
  }
}
