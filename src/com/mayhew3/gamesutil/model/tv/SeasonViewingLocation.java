package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.FieldValueForeignKey;
import com.mayhew3.gamesutil.dataobject.Nullability;

public class SeasonViewingLocation extends RetireableDataObject {

  /* Data */
  public FieldValueForeignKey seasonId = registerForeignKey(new Season(), Nullability.NOT_NULL);
  public FieldValueForeignKey viewingLocationId = registerForeignKey(new ViewingLocation(), Nullability.NOT_NULL);

  public SeasonViewingLocation() {
    addUniqueConstraint(seasonId, viewingLocationId);
  }

  @Override
  protected String getTableName() {
    return "season_viewing_location";
  }

  @Override
  public String toString() {
    return seasonId.getValue() + ", " + viewingLocationId.getValue();
  }

}
