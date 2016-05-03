package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

public class SeriesGenre extends DataObject {

  /* Data */
  public FieldValueForeignKey seriesId = registerForeignKey("series_id", new Series(), Nullability.NOT_NULL);
  public FieldValueForeignKey genreId = registerForeignKey("genre_id", new Genre(), Nullability.NOT_NULL, IntegerSize.SMALLINT);

  public SeriesGenre() {
    addUniqueConstraint(seriesId, genreId);
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
