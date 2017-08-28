package com.mayhew3.mediamogul.dataobject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataObjectMismatch {
  private DataObject dataObject;
  private FieldValue fieldValue;
  private String message;


  public DataObjectMismatch(@NotNull DataObject dataObject, @Nullable FieldValue fieldValue, @NotNull String message) {
    this.dataObject = dataObject;
    this.fieldValue = fieldValue;
    this.message = message;
  }

  public DataObject getDataObject() {
    return dataObject;
  }

  @Nullable
  public FieldValue getFieldValue() {
    return fieldValue;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    String printout = "Table " + dataObject.getTableName();
    if (fieldValue != null) {
      printout += ", Field " + fieldValue.getFieldName();
    }
    return printout + ", Message: " + message;
  }
}
