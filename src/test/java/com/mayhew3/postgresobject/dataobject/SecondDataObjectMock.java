package com.mayhew3.postgresobject.dataobject;

class SecondDataObjectMock extends RetireableDataObject {
  FieldValueString name = registerStringField("name", Nullability.NOT_NULL);
  FieldValueBigDecimal gravy_amt = registerBigDecimalField("gravy_amt", Nullability.NULLABLE);

  SecondDataObjectMock() {
    super();
    addColumnsIndex(name);
  }

  @Override
  public String getTableName() {
    return "test_second";
  }
}
