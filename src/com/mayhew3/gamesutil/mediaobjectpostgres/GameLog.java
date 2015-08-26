package com.mayhew3.gamesutil.mediaobjectpostgres;

import java.sql.Timestamp;

public class GameLog extends MediaObject {


  public FieldValue<Timestamp> eventdate = registerTimestampField("eventdate");

  public FieldValueInteger previousPlaytime = registerIntegerField("previousplaytime");
  public FieldValueInteger updatedplaytime = registerIntegerField("updatedplaytime");
  public FieldValueInteger diff = registerIntegerField("diff");

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
