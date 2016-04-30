package com.mayhew3.gamesutil.dataobject;

public class SteamAttribute extends DataObject {


  public FieldValueInteger steamID = registerIntegerField("steamid", Nullability.NOT_NULL);

  public FieldValueString attribute = registerStringField("attribute", Nullability.NOT_NULL);


  @Override
  protected String getTableName() {
    return "steam_attributes";
  }

  @Override
  public String toString() {
    return "Attribute '" + attribute.getValue() + "' for SteamID: " + steamID.getValue();
  }
}
