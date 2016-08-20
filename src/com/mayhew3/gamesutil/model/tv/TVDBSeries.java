package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

public class TVDBSeries extends DataObject {

  public FieldValueTimestamp firstAired = registerTimestampField("first_aired", Nullability.NULLABLE);

  public FieldValueInteger tvdbSeriesExtId = registerIntegerField("tvdb_series_ext_id", Nullability.NULLABLE);

  public FieldValueInteger ratingCount = registerIntegerField("rating_count", Nullability.NULLABLE);
  public FieldValueInteger runtime = registerIntegerField("runtime", Nullability.NULLABLE);
  public FieldValueBigDecimal rating = registerBigDecimalField("rating", Nullability.NULLABLE);
  public FieldValue<String> name = registerStringField("name", Nullability.NULLABLE);

  public FieldValue<String> airsDayOfWeek = registerStringField("airs_day_of_week", Nullability.NULLABLE);
  public FieldValue<String> airsTime = registerStringField("airs_time", Nullability.NULLABLE);
  public FieldValue<String> network = registerStringField("network", Nullability.NULLABLE);
  public FieldValue<String> overview = registerStringField("overview", Nullability.NULLABLE);
  public FieldValue<String> status = registerStringField("status", Nullability.NULLABLE);
  public FieldValue<String> poster = registerStringField("poster", Nullability.NULLABLE);
  public FieldValue<String> banner = registerStringField("banner", Nullability.NULLABLE);
  public FieldValue<String> lastUpdated = registerStringField("last_updated", Nullability.NULLABLE);
  public FieldValue<String> imdbId = registerStringField("imdb_id", Nullability.NULLABLE);
  public FieldValue<String> zap2it_id = registerStringField("zap2it_id", Nullability.NULLABLE);

  public FieldValueInteger apiVersion = registerIntegerField("api_version", Nullability.NOT_NULL).defaultValue(1);

  public TVDBSeries() {
    addUniqueConstraint(tvdbSeriesExtId);
  }

  @Override
  protected String getTableName() {
    return "tvdb_series";
  }

  @Override
  public String toString() {
    return name.getValue();
  }

}
