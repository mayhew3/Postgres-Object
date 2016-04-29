package com.mayhew3.gamesutil.dataobject;

public class TVDBSeries extends DataObject {

  public FieldValueTimestamp firstAired = registerTimestampField("first_aired");

  public FieldValueInteger tvdbId = registerIntegerField("tvdb_id");
  public FieldValue<String> tvdbSeriesId = registerStringField("tvdb_series_id");
  public FieldValueInteger ratingCount = registerIntegerField("rating_count");
  public FieldValueInteger runtime = registerIntegerField("runtime");
  public FieldValueBigDecimal rating = registerBigDecimalField("rating");
  public FieldValue<String> name = registerStringField("name");

  public FieldValue<String> airsDayOfWeek = registerStringField("airs_day_of_week");
  public FieldValue<String> airsTime = registerStringField("airs_time");
  public FieldValue<String> network = registerStringField("network");
  public FieldValue<String> overview = registerStringField("overview");
  public FieldValue<String> status = registerStringField("status");
  public FieldValue<String> poster = registerStringField("poster");
  public FieldValue<String> banner = registerStringField("banner");
  public FieldValue<String> lastUpdated = registerStringField("last_updated");
  public FieldValue<String> imdbId = registerStringField("imdb_id");
  public FieldValue<String> zap2it_id = registerStringField("zap2it_id");

  @Override
  protected String getTableName() {
    return "tvdb_series";
  }

  @Override
  public String toString() {
    return name.getValue();
  }

}
