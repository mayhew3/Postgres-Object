package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

public class PossibleSeriesMatch extends DataObject {

  /* FK */
  public FieldValueForeignKey seriesId = registerForeignKey(new Series(), Nullability.NOT_NULL);

  /* Data */
  public FieldValueInteger tvdbSeriesExtId = registerIntegerField("tvdb_series_ext_id", Nullability.NOT_NULL);
  public FieldValueString tvdbSeriesTitle = registerStringField("tvdb_series_title", Nullability.NOT_NULL);

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
