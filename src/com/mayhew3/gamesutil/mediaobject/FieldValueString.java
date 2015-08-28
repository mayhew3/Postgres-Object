package com.mayhew3.gamesutil.mediaobject;

public class FieldValueString extends FieldValue<String> {
  public FieldValueString(String fieldName, FieldConversion<String> converter) {
    super(fieldName, converter);
  }

  @Override
  protected void initializeValue(String value) {
    super.initializeValue(value);
    this.isText = true;
  }

  @Override
  protected void initializeValueFromString(String valueString) {
    super.initializeValueFromString(valueString);
    this.isText = true;
  }
}
