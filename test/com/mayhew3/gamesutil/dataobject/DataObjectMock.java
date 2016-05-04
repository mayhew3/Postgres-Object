package com.mayhew3.gamesutil.dataobject;

class DataObjectMock extends DataObject {
  FieldValueString title = registerStringField("title", Nullability.NOT_NULL);
  FieldValueInteger kernels = registerIntegerField("kernels", Nullability.NULLABLE).defaultValue(0);

  DataObjectMock() {
    addUniqueConstraint(title);
    addUniqueConstraint(kernels, dateAdded);
  }

  @Override
  protected String getTableName() {
    return "test";
  }
}
