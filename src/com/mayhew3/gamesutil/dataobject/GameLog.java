package com.mayhew3.gamesutil.dataobject;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class GameLog extends DataObject {


  public FieldValue<Timestamp> eventdate = registerTimestampField("eventdate");

  public FieldValue<BigDecimal> previousPlaytime = registerBigDecimalField("previousplaytime");
  public FieldValue<BigDecimal> updatedplaytime = registerBigDecimalField("updatedplaytime");
  public FieldValue<BigDecimal> diff = registerBigDecimalField("diff");

  public FieldValueInteger steamID = registerIntegerField("steamid");

  public FieldValueString game = registerStringField("game");
  public FieldValueString platform = registerStringField("platform");
  public FieldValueString eventtype = registerStringField("eventtype");


  @Override
  protected String getTableName() {
    return "gamelogs";
  }

  @Override
  public String toString() {
    return game.getValue() + ": " + eventdate.getValue() + " (" + diff.getValue() + " minutes)";
  }
}
