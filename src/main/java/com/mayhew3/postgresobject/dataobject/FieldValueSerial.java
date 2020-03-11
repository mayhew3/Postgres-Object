package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.DatabaseType;

@SuppressWarnings("WeakerAccess")
public class FieldValueSerial extends FieldValueInteger {
  private String sequenceName;

  FieldValueSerial(String fieldName, FieldConversion<Integer> converter, Nullability nullability, String sequenceName) {
    super(fieldName, converter, nullability);
    this.sequenceName = sequenceName;
  }

  @Override
  public String getInformationSchemaDefault(DatabaseType databaseType) {
    if (databaseType == DatabaseType.POSTGRES) {
      return "nextval('" + sequenceName + "'::regclass)";
    } else {
      return null;
    }
  }
}
