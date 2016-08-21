package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.DataObject;
import com.mayhew3.gamesutil.dataobject.FieldValueForeignKey;
import com.mayhew3.gamesutil.dataobject.FieldValueString;
import com.mayhew3.gamesutil.dataobject.Nullability;

public class TVDBMigrationError extends DataObject {

  /* Foreign Keys */
  public FieldValueForeignKey tvdbSeriesId = registerForeignKey(new TVDBSeries(), Nullability.NOT_NULL);
  public FieldValueForeignKey tvdbEpisodeId = registerForeignKey(new TVDBEpisode(), Nullability.NULLABLE);

  public FieldValueString exceptionType = registerStringField("exception_type", Nullability.NOT_NULL);
  public FieldValueString exceptionMsg = registerStringField("exception_msg", Nullability.NOT_NULL);


  @Override
  protected String getTableName() {
    return "tvdb_migration_error";
  }
}
