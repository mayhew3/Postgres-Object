package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.FieldValueForeignKey;
import com.mayhew3.gamesutil.dataobject.FieldValueInteger;
import com.mayhew3.gamesutil.dataobject.FieldValueTimestamp;
import com.mayhew3.gamesutil.dataobject.Nullability;

public class TVDBWorkItem extends RetireableDataObject {

  public FieldValueInteger tvdbSeriesExtId = registerIntegerField("tvdb_series_ext_id", Nullability.NOT_NULL);
  public FieldValueForeignKey seriesId = registerForeignKey(new Series(), Nullability.NOT_NULL);

  public FieldValueTimestamp foundTime = registerTimestampField("found_time", Nullability.NOT_NULL);
  public FieldValueTimestamp lastUpdated = registerTimestampField("last_updated", Nullability.NOT_NULL);
  public FieldValueTimestamp processedTime = registerTimestampField("processed_time", Nullability.NULLABLE);

  @Override
  protected String getTableName() {
    return "tvdb_work_item";
  }
}
