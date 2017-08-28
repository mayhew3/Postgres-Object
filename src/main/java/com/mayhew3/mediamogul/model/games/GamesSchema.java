package com.mayhew3.mediamogul.model.games;

import com.mayhew3.mediamogul.dataobject.DataSchema;

public class GamesSchema {

  public static DataSchema games_schema = new DataSchema(
      new Game(),
      new GameLog(),
      new SteamAttribute()
  );

}
