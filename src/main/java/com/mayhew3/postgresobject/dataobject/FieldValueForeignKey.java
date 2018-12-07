package com.mayhew3.postgresobject.dataobject;

public class FieldValueForeignKey extends FieldValueInteger {
  private String tableName;

  public FieldValueForeignKey(String fieldName, FieldConversion<Integer> converter, Nullability nullability, DataObject dataObject) {
    super(fieldName, converter, nullability);
    this.tableName = dataObject.getTableName();
  }

  public String getTableName() {
    return tableName;
  }
}
