package com.mayhew3.gamesutil;

public class FieldValue<T> {
  private String fieldName;
  private T value;
  private FieldConversion<T> converter;

  public FieldValue(String fieldName, FieldConversion<T> converter) {
    this.fieldName = fieldName;
    this.converter = converter;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public void setValueFromString(String valueString) {
    this.value = converter.setValue(valueString);
  }

  public String getFieldName() {
    return fieldName;
  }
}
