package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.DataObject;
import com.mayhew3.gamesutil.dataobject.FieldValueString;
import com.mayhew3.gamesutil.dataobject.Nullability;

public class TVDBUpdateErrorType extends DataObject {

  public FieldValueString exceptionClass = registerStringField("exception_class", Nullability.NOT_NULL);


  @Override
  public String getTableName() {
    return "tvdb_update_error_type";
  }

  @Override
  public String toString() {
    return "TVDB Error Type " + id.getValue() + ": " + exceptionClass.getValue();
  }
}
