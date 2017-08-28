package com.mayhew3.mediamogul.model.games;

import com.mayhew3.mediamogul.dataobject.DataObject;
import com.mayhew3.mediamogul.dataobject.FieldValueInteger;
import com.mayhew3.mediamogul.dataobject.FieldValueString;
import com.mayhew3.mediamogul.dataobject.Nullability;

public class SteamAttribute extends DataObject {


  public FieldValueInteger steamID = registerIntegerField("steamid", Nullability.NOT_NULL);

  public FieldValueString attribute = registerStringField("attribute", Nullability.NOT_NULL);


  @Override
  public String getTableName() {
    return "steam_attributes";
  }

  @Override
  public String toString() {
    return "Attribute '" + attribute.getValue() + "' for SteamID: " + steamID.getValue();
  }
}
