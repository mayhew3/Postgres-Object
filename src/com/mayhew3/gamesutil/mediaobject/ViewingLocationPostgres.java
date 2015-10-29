package com.mayhew3.gamesutil.mediaobject;

public class ViewingLocationPostgres extends MediaObjectPostgreSQL {

  /* Data */
  public FieldValueString viewingLocationName = registerStringField("name");

  @Override
  protected String getTableName() {
    return "viewing_location";
  }

  @Override
  public String toString() {
    return viewingLocationName.getValue();
  }

}
