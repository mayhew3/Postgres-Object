package com.mayhew3.gamesutil.mediaobjectpostgres;

public class FieldValueShort extends FieldValue<Short> {
  public FieldValueShort(String fieldName, FieldConversion<Short> converter) {
    super(fieldName, converter);
  }

  public void increment(Short numberToAdd) {
    Short value = getValue();
    if (value == null) {
      value = 0;
    }
    value = (short) (value + numberToAdd);
    changeValue(value);
  }
}
