package com.mayhew3.gamesutil.dataobject;

public class SeasonViewingLocation extends DataObject {

  /* Data */
  public FieldValueInteger seasonId = registerIntegerField("season_id");
  public FieldValueInteger viewingLocationId = registerIntegerField("viewing_location_id");

  @Override
  protected String getTableName() {
    return "season_viewing_location";
  }

  @Override
  public String toString() {
    return seasonId.getValue() + ", " + viewingLocationId.getValue();
  }

}
