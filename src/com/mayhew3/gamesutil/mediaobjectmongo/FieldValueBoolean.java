package com.mayhew3.gamesutil.mediaobjectmongo;

public class FieldValueBoolean extends FieldValue<Boolean> {
  public FieldValueBoolean(String fieldName, FieldConversion<Boolean> converter) {
    super(fieldName, converter);
  }

  @Override
  public void initializeValue(Boolean value) {
    super.initializeValue((value == null) ? false : value);
  }
}
