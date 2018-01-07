package com.mayhew3.mediamogul.model.tv;

import com.mayhew3.mediamogul.dataobject.DataObject;
import com.mayhew3.mediamogul.dataobject.FieldValueBoolean;
import com.mayhew3.mediamogul.dataobject.FieldValueInteger;
import com.mayhew3.mediamogul.dataobject.Nullability;

public class SystemVars extends DataObject {

  FieldValueInteger ratingYear = registerIntegerField("rating_year", Nullability.NOT_NULL);
  public FieldValueBoolean ratingLocked = registerBooleanField("rating_locked", Nullability.NOT_NULL);

  @Override
  public String getTableName() {
    return "system_vars";
  }
}
