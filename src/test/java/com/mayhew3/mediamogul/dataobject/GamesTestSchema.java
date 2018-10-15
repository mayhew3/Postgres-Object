package com.mayhew3.mediamogul.dataobject;

import com.mayhew3.mediamogul.model.games.GamesSchema;

public class GamesTestSchema {
  public static DataSchema games_test_schema =
      new DataSchema(GamesSchema.games_schema.getAllTables()).addDataObject(new DataObjectMock());
}
