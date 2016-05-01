package com.mayhew3.gamesutil.model.games;

import com.mayhew3.gamesutil.dataobject.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class GameLog extends DataObject {


  public FieldValue<Timestamp> eventdate = registerTimestampField("eventdate", Nullability.NOT_NULL);

  public FieldValue<BigDecimal> previousPlaytime = registerBigDecimalField("previousplaytime", Nullability.NULLABLE);
  public FieldValue<BigDecimal> updatedplaytime = registerBigDecimalField("updatedplaytime", Nullability.NULLABLE);
  public FieldValue<BigDecimal> diff = registerBigDecimalField("diff", Nullability.NULLABLE);

  public FieldValueInteger steamID = registerIntegerField("steamid", Nullability.NULLABLE);

  public FieldValueString game = registerStringField("game", Nullability.NOT_NULL);
  public FieldValueString platform = registerStringField("platform", Nullability.NOT_NULL);
  public FieldValueString eventtype = registerStringField("eventtype", Nullability.NULLABLE);


  @Override
  protected String getTableName() {
    return "gamelogs";
  }

  @Override
  public String toString() {
    return game.getValue() + ": " + eventdate.getValue() + " (" + diff.getValue() + " minutes)";
  }
}
