package com.mayhew3.gamesutil;

import java.util.Objects;

public class FieldValue<T> {
  private String fieldName;
  private T originalValue;
  private T changedValue;
  private FieldConversion<T> converter;

  public FieldValue(String fieldName, FieldConversion<T> converter) {
    this.fieldName = fieldName;
    this.converter = converter;
  }

  public T getOriginalValue() {
    return originalValue;
  }

  public T getValue() {
    return changedValue == null ? originalValue : changedValue;
  }

  protected void initializeValue(T value) {
    this.originalValue = value;
  }

  protected void initializeValueFromString(String valueString) {
    this.originalValue = converter.setValue(valueString);
  }

  public String getFieldName() {
    return fieldName;
  }


  public T getChangedValue() {
    return changedValue;
  }

  public void changeValue(T newValue) {
    changedValue = newValue;
  }

  public void discardChange() {
    changedValue = null;
  }

  public Boolean isChanged() {
    return changedValue != null && !Objects.equals(originalValue, changedValue);
  }

  public void updateInternal() {
    originalValue = changedValue;
    changedValue = null;
  }

  @Override
  public String toString() {
    String displayString = "'" + fieldName + "': " + originalValue;
    if (isChanged()) {
      displayString += " -> " + changedValue;
    }
    return displayString;
  }
}
