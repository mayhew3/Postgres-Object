package com.mayhew3.gamesutil.dataobject;

public class MetacriticSeason extends DataObject {

  /* Data */
  public FieldValueInteger seriesId = registerIntegerField("series_id");
  public FieldValueInteger season = registerIntegerField("season");
  public FieldValueInteger metacritic = registerIntegerField("metacritic");

  @Override
  protected String getTableName() {
    return "metacritic_season";
  }

  @Override
  public String toString() {
    return "Metacritic Season " + season.getValue() + " for series (" + seriesId.getValue() + ")";
  }

}
