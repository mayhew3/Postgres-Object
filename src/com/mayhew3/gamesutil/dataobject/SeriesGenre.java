package com.mayhew3.gamesutil.dataobject;

public class SeriesGenre extends DataObject {

  /* Data */
  public FieldValueInteger seriesId = registerIntegerField("series_id", Nullability.NOT_NULL);
  public FieldValueInteger genreId = registerIntegerField("genre_id", Nullability.NOT_NULL, IntegerSize.SMALLINT);

  public SeriesGenre() {
    addUniqueConstraint().addField(seriesId).addField(genreId);
  }

  @Override
  protected String getTableName() {
    return "series_genre";
  }

  @Override
  public String toString() {
    return seriesId.getValue() + ", " + genreId.getValue();
  }

}
