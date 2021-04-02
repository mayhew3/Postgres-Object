package com.mayhew3.postgresobject.dataobject;

public class DataSchemaMock {
  public static DataSchema schemaDef = new DataSchema(
      new DataObjectMock(),
      new SecondDataObjectMock()
  );
}
