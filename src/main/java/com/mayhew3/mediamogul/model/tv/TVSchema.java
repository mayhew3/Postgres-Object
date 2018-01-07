package com.mayhew3.mediamogul.model.tv;

import com.mayhew3.mediamogul.dataobject.DataSchema;
import com.mayhew3.mediamogul.model.Person;

public class TVSchema {

  public static DataSchema tv_schema = new DataSchema(
      new ConnectLog(),
      new EdgeTiVoEpisode(),
      new Episode(),
      new EpisodeGroupRating(),
      new EpisodeRating(),
      new ErrorLog(),
      new Genre(),
      new Movie(),
      new Person(),
      new PersonSeries(),
      new PossibleSeriesMatch(),
      new PossibleEpisodeMatch(),
      new Season(),
      new SeasonViewingLocation(),
      new Series(),
      new SeriesGenre(),
      new SeriesViewingLocation(),
      new SystemVars(),
      new TiVoEpisode(),
      new TmpRating(),
      new TVDBEpisode(),
      new TVDBPoster(),
      new TVDBSeries(),
      new TVDBMigrationError(),
      new TVDBMigrationLog(),
      new TVDBConnectionLog(),
      new TVDBUpdateError(),
      new TVDBWorkItem(),
      new ViewingLocation()
  );

}
