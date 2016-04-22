package com.mayhew3.gamesutil.dataobject;

public class SeriesGenre extends DataObject {

  /* Data */
  public FieldValueInteger seriesId = registerIntegerField("series_id");
  public FieldValueInteger genreId = registerIntegerField("genre_id");

  @Override
  protected String getTableName() {
    return "series_genre";
  }

  @Override
  public String toString() {
    return seriesId.getValue() + ", " + genreId.getValue();
  }

}
