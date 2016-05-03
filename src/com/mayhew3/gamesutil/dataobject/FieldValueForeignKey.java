package com.mayhew3.gamesutil.dataobject;

public class FieldValueForeignKey extends FieldValueInteger {
  private String tableName;

  public FieldValueForeignKey(String fieldName, FieldConversion<Integer> converter, Nullability nullability, DataObject dataObject) {
    super(fieldName, converter, nullability);
    this.tableName = dataObject.getTableName();
  }

  public FieldValueForeignKey(String fieldName, FieldConversion<Integer> converter, Nullability nullability, DataObject dataObject, IntegerSize integerSize) {
    super(fieldName, converter, nullability, integerSize);
    this.tableName = dataObject.getTableName();
  }

  public String getTableName() {
    return tableName;
  }
}
