package com.mayhew3.postgresobject.dataobject;

class DataObjectMock extends DataObject {
  FieldValueString title = registerStringField("title", Nullability.NOT_NULL);
  FieldValueInteger kernels = registerIntegerField("kernels", Nullability.NULLABLE).defaultValue(0);

  DataObjectMock() {
    addUniqueConstraint(title);
    addUniqueConstraint(kernels, dateAdded);
    addColumnsIndex(title, kernels);
  }

  @Override
  void preInsert() {
    // nothing to do
  }

  @Override
  public String getTableName() {
    return "test";
  }
}
