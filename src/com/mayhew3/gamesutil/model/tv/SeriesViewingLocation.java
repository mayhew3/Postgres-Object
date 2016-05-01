package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.DataObject;
import com.mayhew3.gamesutil.dataobject.FieldValueInteger;
import com.mayhew3.gamesutil.dataobject.IntegerSize;
import com.mayhew3.gamesutil.dataobject.Nullability;

public class SeriesViewingLocation extends DataObject {

  /* Data */
  public FieldValueInteger seriesId = registerIntegerField("series_id", Nullability.NOT_NULL);
  public FieldValueInteger viewingLocationId = registerIntegerField("viewing_location_id", Nullability.NOT_NULL, IntegerSize.SMALLINT);

  @Override
  protected String getTableName() {
    return "series_viewing_location";
  }

  @Override
  public String toString() {
    return seriesId.getValue() + ", " + viewingLocationId.getValue();
  }

}
