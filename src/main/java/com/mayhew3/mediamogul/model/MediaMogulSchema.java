package com.mayhew3.mediamogul.model;

import com.mayhew3.mediamogul.dataobject.DataSchema;
import com.mayhew3.mediamogul.model.Person;
import com.mayhew3.mediamogul.model.games.*;
import com.mayhew3.mediamogul.model.tv.*;

public class MediaMogulSchema {

  public static DataSchema schema = new DataSchema(
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
      new TVGroup(),
      new TVGroupPerson(),
      new TVGroupSeries(),
      new TVDBEpisode(),
      new TVDBPoster(),
      new TVDBSeries(),
      new TVDBMigrationError(),
      new TVDBMigrationLog(),
      new TVDBConnectionLog(),
      new TVDBUpdateError(),
      new TVDBWorkItem(),
      new ViewingLocation(),
      new Game(),
      new GameLog(),
      new GameplaySession(),
      new SteamAttribute(),
      new PossibleGameMatch(),
      new PersonGame()
  );

}
