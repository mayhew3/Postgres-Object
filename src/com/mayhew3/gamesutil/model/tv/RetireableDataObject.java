package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.DataObject;
import com.mayhew3.gamesutil.dataobject.FieldValueInteger;
import com.mayhew3.gamesutil.dataobject.Nullability;

public abstract class RetireableDataObject extends DataObject {
  public FieldValueInteger retired = registerIntegerField("retired", Nullability.NOT_NULL).defaultValue(0);
}

