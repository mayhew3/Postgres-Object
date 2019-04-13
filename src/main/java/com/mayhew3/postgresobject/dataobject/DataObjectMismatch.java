package com.mayhew3.postgresobject.dataobject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataObjectMismatch {
  private DataObject dataObject;
  private FieldValue fieldValue;
  private ColumnsIndex columnsIndex;
  private String message;


  public DataObjectMismatch(@NotNull DataObject dataObject, @NotNull String message) {
    this.dataObject = dataObject;
    this.message = message;
  }

  public DataObjectMismatch withFieldValue(FieldValue fieldValue) {
    this.fieldValue = fieldValue;
    return this;
  }

  public DataObjectMismatch withColumnsIndex(ColumnsIndex index) {
    this.columnsIndex = index;
    return this;
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
    if (columnsIndex != null) {
      printout += ", Index " + columnsIndex.getIndexName();
    }
    return printout + ", Message: " + message;
  }
}
