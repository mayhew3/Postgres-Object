package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.FieldValueInteger;
import com.mayhew3.gamesutil.dataobject.FieldValueString;
import com.mayhew3.gamesutil.dataobject.Nullability;
import com.mayhew3.gamesutil.dataobject.RetireableDataObject;

public class TVDBPoster extends RetireableDataObject {

  public FieldValueString posterPath = registerStringField("poster_path", Nullability.NOT_NULL);
  public FieldValueInteger tvdb_series_id = registerForeignKey(new TVDBSeries(), Nullability.NOT_NULL);
  public FieldValueInteger season = registerIntegerField("season", Nullability.NULLABLE);

  @Override
  public String getTableName() {
    return "tvdb_poster";
  }
}
