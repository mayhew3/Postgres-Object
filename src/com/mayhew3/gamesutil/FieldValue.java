package com.mayhew3.gamesutil;

import java.util.Objects;

public class FieldValue<T> {
  private String fieldName;
  private T originalValue;
  private T changedValue;
  private FieldConversion<T> converter;

  private Boolean wasText = false;
  protected Boolean isText = false;

  public FieldValue(String fieldName, FieldConversion<T> converter) {
    this.fieldName = fieldName;
    this.converter = converter;
  }

  public T getOriginalValue() {
    return originalValue;
  }

  public T getValue() {
    return changedValue;
  }

  protected void initializeValue(T value) {
    this.originalValue = value;
    this.changedValue = value;
  }

  protected void initializeValueFromString(String valueString) {
    T convertedValue = getConversion(valueString);
    initializeValue(convertedValue);

    this.wasText = true;
  }

  private T getConversion(String valueString) {
    try {
      return converter.parseFromString(valueString);
    } catch (NumberFormatException e) {
      throw new RuntimeException("Error converting " + fieldName + " field with value " + valueString + " to Number.");
    }
  }

  public void changeValueFromString(String valueString) {
    this.changedValue = getConversion(valueString);
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
    changedValue = originalValue;
  }

  public Boolean isChanged() {
    return shouldUpgradeText() || valueHasChanged();
  }

  private boolean valueHasChanged() {
    return !Objects.equals(originalValue, changedValue);
  }

  protected boolean shouldUpgradeText() {
    return (originalValue != null && wasText && !isText);
  }

  public void updateInternal() {
    originalValue = changedValue;
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
