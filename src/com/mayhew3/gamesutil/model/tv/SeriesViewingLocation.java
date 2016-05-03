package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

public class SeriesViewingLocation extends DataObject {

  /* Data */
  public FieldValueForeignKey seriesId = registerForeignKey("series_id", new Series(), Nullability.NOT_NULL);
  public FieldValueForeignKey viewingLocationId = registerForeignKey("viewing_location_id", new ViewingLocation(), Nullability.NOT_NULL, IntegerSize.SMALLINT);

  @Override
  protected String getTableName() {
    return "series_viewing_location";
  }

  @Override
  public String toString() {
    return seriesId.getValue() + ", " + viewingLocationId.getValue();
  }

}
