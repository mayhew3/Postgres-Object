package com.mayhew3.gamesutil.mediaobject;

public class FieldValueBoolean extends FieldValue<Boolean> {
  private Boolean allowNulls = false;

  public FieldValueBoolean(String fieldName, FieldConversion<Boolean> converter) {
    super(fieldName, converter);
  }
  public FieldValueBoolean(String fieldName, FieldConversion<Boolean> converter, Boolean allowNulls) {
    super(fieldName, converter);
    this.allowNulls = allowNulls;
  }

  @Override
  public void initializeValue(Boolean value) {
    if (allowNulls) {
      super.initializeValue(value);
    } else {
      super.initializeValue((value == null) ? false : value);
    }
  }
}
