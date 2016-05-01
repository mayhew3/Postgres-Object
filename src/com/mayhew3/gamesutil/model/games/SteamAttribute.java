package com.mayhew3.gamesutil.model.games;

import com.mayhew3.gamesutil.dataobject.DataObject;
import com.mayhew3.gamesutil.dataobject.FieldValueInteger;
import com.mayhew3.gamesutil.dataobject.FieldValueString;
import com.mayhew3.gamesutil.dataobject.Nullability;

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
