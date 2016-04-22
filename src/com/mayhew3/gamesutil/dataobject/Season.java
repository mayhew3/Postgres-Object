package com.mayhew3.gamesutil.dataobject;

public class Season extends DataObject {

  /* FK */
  public FieldValueInteger seriesId = registerIntegerField("series_id");

  /* Data */
  public FieldValueInteger seasonNumber = registerIntegerField("season_number");
  public FieldValueInteger metacritic = registerIntegerField("metacritic");


  @Override
  protected String getTableName() {
    return "season";
  }

  @Override
  public String toString() {
    return seriesId.getValue() + ", Season " + seasonNumber.getValue();
  }

}
