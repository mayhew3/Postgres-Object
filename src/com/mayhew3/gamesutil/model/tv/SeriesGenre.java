package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.DataObject;
import com.mayhew3.gamesutil.dataobject.FieldValueInteger;
import com.mayhew3.gamesutil.dataobject.IntegerSize;
import com.mayhew3.gamesutil.dataobject.Nullability;

public class SeriesGenre extends DataObject {

  /* Data */
  public FieldValueInteger seriesId = registerIntegerField("series_id", Nullability.NOT_NULL);
  public FieldValueInteger genreId = registerIntegerField("genre_id", Nullability.NOT_NULL, IntegerSize.SMALLINT);

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
