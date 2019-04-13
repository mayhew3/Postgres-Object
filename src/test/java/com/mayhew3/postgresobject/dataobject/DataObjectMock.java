package com.mayhew3.postgresobject.dataobject;

class DataObjectMock extends DataObject {
  FieldValueString title = registerStringField("title", Nullability.NOT_NULL);
  FieldValueInteger kernels = registerIntegerField("kernels", Nullability.NULLABLE).defaultValue(0);

  DataObjectMock() {
    addUniqueConstraint(1, title);
    addUniqueConstraint(2, kernels, dateAdded);
    addColumnsIndex(3, title, kernels);
  }

  @Override
  public String getTableName() {
    return "test";
  }
}
