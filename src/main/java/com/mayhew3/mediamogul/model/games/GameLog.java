package com.mayhew3.mediamogul.model.games;

import com.mayhew3.mediamogul.dataobject.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class GameLog extends DataObject {


  public FieldValueTimestamp eventdate = registerTimestampField("eventdate", Nullability.NOT_NULL);

  public FieldValueBigDecimal previousPlaytime = registerBigDecimalField("previousplaytime", Nullability.NULLABLE);
  public FieldValueBigDecimal updatedplaytime = registerBigDecimalField("updatedplaytime", Nullability.NULLABLE);
  public FieldValueBigDecimal diff = registerBigDecimalField("diff", Nullability.NULLABLE);

  public FieldValueInteger steamID = registerIntegerField("steamid", Nullability.NULLABLE);

  public FieldValueString game = registerStringField("game", Nullability.NOT_NULL);
  public FieldValueString platform = registerStringField("platform", Nullability.NOT_NULL);
  public FieldValueString eventtype = registerStringField("eventtype", Nullability.NULLABLE);

  public FieldValueForeignKey gameID = registerForeignKey(new Game(), Nullability.NOT_NULL);

  public FieldValueForeignKey gameplaySessionID = registerForeignKey(new GameplaySession(), Nullability.NULLABLE);

  @Override
  public String getTableName() {
    return "game_log";
  }

  @Override
  public String toString() {
    return game.getValue() + ": " + eventdate.getValue() + " (" + diff.getValue() + " minutes)";
  }
}
