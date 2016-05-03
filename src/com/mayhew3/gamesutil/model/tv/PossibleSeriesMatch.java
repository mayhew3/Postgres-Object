package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

public class PossibleSeriesMatch extends DataObject {

  /* FK */
  public FieldValueForeignKey seriesId = registerForeignKey("series_id", new Series(), Nullability.NOT_NULL);

  /* Data */
  // todo: change to tvdb_series_remote_id
  public FieldValueInteger tvdbSeriesId = registerIntegerField("tvdb_series_id", Nullability.NOT_NULL);
  public FieldValueString tvdbSeriesTitle = registerStringField("tvdb_series_title", Nullability.NOT_NULL);

  public PossibleSeriesMatch() {
    addUniqueConstraint(seriesId, tvdbSeriesId);
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
