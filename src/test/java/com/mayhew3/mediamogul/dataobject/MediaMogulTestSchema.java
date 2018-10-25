package com.mayhew3.mediamogul.dataobject;

import com.mayhew3.mediamogul.model.MediaMogulSchema;

public class MediaMogulTestSchema {
  public static DataSchema test_schema =
      new DataSchema(MediaMogulSchema.schema.getAllTables()).addDataObject(new DataObjectMock());
}
