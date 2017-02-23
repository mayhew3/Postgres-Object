package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

public class SeriesViewingLocation extends RetireableDataObject {

  /* Data */
  public FieldValueForeignKey seriesId = registerForeignKey(new Series(), Nullability.NOT_NULL);
  public FieldValueForeignKey viewingLocationId = registerForeignKey(new ViewingLocation(), Nullability.NOT_NULL);

  @Override
  protected String getTableName() {
    return "series_viewing_location";
  }

  @Override
  public String toString() {
    return seriesId.getValue() + ", " + viewingLocationId.getValue();
  }

}
