package com.mayhew3.gamesutil.dataobject;

public class Season extends DataObject {

  /* FK */
  public FieldValueInteger seriesId = registerIntegerField("series_id", Nullability.NOT_NULL);

  /* Data */
  public FieldValueInteger seasonNumber = registerIntegerField("season_number", Nullability.NOT_NULL, IntegerSize.SMALLINT);
  public FieldValueInteger metacritic = registerIntegerField("metacritic", Nullability.NULLABLE, IntegerSize.SMALLINT);

  public FieldValueTimestamp dateModified = registerTimestampField("date_modified", Nullability.NULLABLE);

  @Override
  protected String getTableName() {
    return "season";
  }

  @Override
  public String toString() {
    return seriesId.getValue() + ", Season " + seasonNumber.getValue();
  }

}
