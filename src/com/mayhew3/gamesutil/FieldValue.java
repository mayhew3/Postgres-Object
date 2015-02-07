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
    return changedValue == null ? originalValue : changedValue;
  }

  protected void initializeValue(T value) {
    this.originalValue = value;
  }

  protected void initializeValueFromString(String valueString) {
    this.originalValue = converter.parseFromString(valueString);
    this.wasText = true;
  }

  public void changeValueFromString(String valueString) {
    this.changedValue = converter.parseFromString(valueString);
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
    return shouldUpgradeText() || valueHasChanged();
  }

  // todo: come up with way to signify the value should be made null from non-null. Might just be
  // todo: better to have changedValue start as same as original.
  private boolean valueHasChanged() {
    return (changedValue != null && !Objects.equals(originalValue, changedValue));
  }

  protected boolean shouldUpgradeText() {
    return (originalValue != null && wasText && !isText);
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
