package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.DataSchema;

public class TVSchema {

  public static DataSchema tv_schema = new DataSchema(
      new ConnectLog(),
      new EdgeTiVoEpisode(),
      new Episode(),
      new ErrorLog(),
      new Genre(),
      new Movie(),
      new PossibleSeriesMatch(),
      new Season(),
      new SeasonViewingLocation(),
      new Series(),
      new SeriesGenre(),
      new SeriesViewingLocation(),
      new TiVoEpisode(),
      new TVDBEpisode(),
      new TVDBSeries(),
      new ViewingLocation()
  );

}
