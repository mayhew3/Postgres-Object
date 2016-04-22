package com.mayhew3.gamesutil.dataobject;

public class Movie extends DataObject {

  public FieldValue<Boolean> onTiVo = registerBooleanField("on_tivo");

  public FieldValueString title = registerStringField("title");
  public FieldValueInteger retired = registerIntegerField("retired");

  public FieldValueTimestamp watchedDate = registerTimestampField("watched_date");
  public FieldValue<Boolean> watched = registerBooleanField("watched");

  public FieldValueTimestamp dateAdded = registerTimestampField("date_added");

  public FieldValueInteger tier = registerIntegerField("tier");
  public FieldValueInteger metacritic = registerIntegerField("metacritic");
  public FieldValueInteger my_rating = registerIntegerField("my_rating");
  public FieldValueString metacriticHint = registerStringField("metacritic_hint");
  public FieldValueString tivoName = registerStringField("tivo_name");


  public FieldValueBoolean isSuggestion = registerBooleanField("suggestion");

  public FieldValueTimestamp showingStartTime = registerTimestampField("showing_start_time");
  public FieldValueTimestamp deletedDate = registerTimestampField("deleted_date");
  public FieldValueTimestamp captureDate = registerTimestampField("capture_date");

  public FieldValue<Boolean> hd = registerBooleanField("hd");

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
    return "movie";
  }

  @Override
  public String toString() {
    return title.getValue();
  }

}
