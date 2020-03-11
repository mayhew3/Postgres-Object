package com.mayhew3.postgresobject.dataobject;

public class MockMySQLSchema extends DataSchema {
  public static DataSchema schema = new DataSchema(
      new CBSId(),
      new TmpDraftAverages(),
      new Player()
  );
}
