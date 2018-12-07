package com.mayhew3.postgresobject.dataobject;

@SuppressWarnings("WeakerAccess")
public class FieldValueSerial extends FieldValueInteger {
  private String sequenceName;

  FieldValueSerial(String fieldName, FieldConversion<Integer> converter, Nullability nullability, String sequenceName) {
    super(fieldName, converter, nullability);
    this.sequenceName = sequenceName;
  }

  @Override
  public String getInformationSchemaDefault() {
    return "nextval('" + sequenceName + "'::regclass)";
  }
}
