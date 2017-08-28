package com.mayhew3.mediamogul.dataobject;

import com.mayhew3.mediamogul.model.tv.TVSchema;

public class TVTestSchema {
  public static DataSchema tv_test_schema =
      new DataSchema(TVSchema.tv_schema.getAllTables()).addDataObject(new DataObjectMock());
}
