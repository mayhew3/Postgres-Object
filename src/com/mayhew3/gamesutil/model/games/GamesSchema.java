package com.mayhew3.gamesutil.model.games;

import com.mayhew3.gamesutil.dataobject.DataSchema;

public class GamesSchema {

  public static DataSchema games_schema = new DataSchema(
      new Game(),
      new GameLog(),
      new SteamAttribute()
  );

}
