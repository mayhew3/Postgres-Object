package com.mayhew3.gamesutil.mediaobject;

public class FieldValueInteger extends FieldValue<Integer> {
  public FieldValueInteger(String fieldName, FieldConversion<Integer> converter) {
    super(fieldName, converter);
  }

  public void increment(Integer numberToAdd) {
    Integer value = getValue();
    if (value == null) {
      value = 0;
    }
    value += numberToAdd;
    changeValue(value);
  }
}
