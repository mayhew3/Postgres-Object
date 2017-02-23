package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.FieldValueForeignKey;
import com.mayhew3.gamesutil.dataobject.FieldValueInteger;
import com.mayhew3.gamesutil.dataobject.FieldValueString;
import com.mayhew3.gamesutil.dataobject.Nullability;

public class TVDBMigrationError extends RetireableDataObject {

  /* Foreign Keys */
  public FieldValueForeignKey seriesId = registerForeignKey(new Series(), Nullability.NOT_NULL);
  public FieldValueInteger tvdbEpisodeExtId = registerIntegerField("tvdb_episode_ext_id", Nullability.NULLABLE);

  public FieldValueString exceptionType = registerStringField("exception_type", Nullability.NOT_NULL);
  public FieldValueString exceptionMsg = registerStringField("exception_msg", Nullability.NOT_NULL);


  @Override
  protected String getTableName() {
    return "tvdb_migration_error";
  }
}
