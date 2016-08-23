package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

public class TVDBMigrationError extends DataObject {

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
