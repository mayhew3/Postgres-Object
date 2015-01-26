package com.mayhew3.gamesutil;

public class FieldValueBoolean extends FieldValue<Boolean> {
  public FieldValueBoolean(String fieldName, FieldConversion<Boolean> converter) {
    super(fieldName, converter);
  }

  @Override
  public void setValue(Boolean value) {
    super.setValue((value == null) ? false : value);
  }
}
