package com.mayhew3.gamesutil.dataobject;

public class PossibleSeriesMatchPostgres extends DataObject {

  /* FK */
  public FieldValueInteger seriesId = registerIntegerField("series_id");

  /* Data */
  public FieldValueInteger tvdbSeriesId = registerIntegerField("tvdb_series_id");
  public FieldValueString tvdbSeriesTitle = registerStringField("tvdb_series_title");

  public FieldValueTimestamp dateAdded = registerTimestampField("date_added");

  @Override
  protected String getTableName() {
    return "possible_series_match";
  }

  @Override
  public String toString() {
    return seriesId.getValue() + ", Title " + tvdbSeriesTitle.getValue();
  }

}
