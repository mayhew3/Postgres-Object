package com.mayhew3.gamesutil.dataobject;

public class SeriesViewingLocationPostgres extends DataObject {

  /* Data */
  public FieldValueInteger seriesId = registerIntegerField("series_id");
  public FieldValueInteger viewingLocationId = registerIntegerField("viewing_location_id");

  @Override
  protected String getTableName() {
    return "series_viewing_location";
  }

  @Override
  public String toString() {
    return seriesId.getValue() + ", " + viewingLocationId.getValue();
  }

}
