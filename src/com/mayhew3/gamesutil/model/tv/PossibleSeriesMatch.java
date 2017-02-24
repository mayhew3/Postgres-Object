package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

public class PossibleSeriesMatch extends RetireableDataObject {

  /* FK */
  public FieldValueForeignKey seriesId = registerForeignKey(new Series(), Nullability.NOT_NULL);

  /* Data */
  public FieldValueInteger tvdbSeriesExtId = registerIntegerField("tvdb_series_ext_id", Nullability.NOT_NULL);
  public FieldValueString tvdbSeriesTitle = registerStringField("tvdb_series_title", Nullability.NOT_NULL);
  public FieldValueString poster = registerStringField("poster", Nullability.NULLABLE);
  public FieldValueBoolean alreadyExists = registerBooleanField("already_exists", Nullability.NOT_NULL).defaultValue(false);

  public PossibleSeriesMatch() {
    addUniqueConstraint(seriesId, tvdbSeriesExtId);
  }

  @Override
  protected String getTableName() {
    return "possible_series_match";
  }

  @Override
  public String toString() {
    return seriesId.getValue() + ", Title " + tvdbSeriesTitle.getValue();
  }

}
