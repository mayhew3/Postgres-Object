package com.mayhew3.gamesutil.mediaobject;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Game extends MediaObjectPostgreSQL {

  public FieldValue<Timestamp> finished = registerTimestampField("finished");
  public FieldValue<Timestamp> added = registerTimestampField("added");

  public FieldValue<BigDecimal> metacritic = registerBigDecimalField("metacritic");
  public FieldValue<BigDecimal> guess = registerBigDecimalField("guess");
  public FieldValue<BigDecimal> mayhew = registerBigDecimalField("mayhew");
  public FieldValue<BigDecimal> sum = registerBigDecimalField("sum");
  public FieldValue<BigDecimal> sumd = registerBigDecimalField("sumd");
  public FieldValue<BigDecimal> timeplayed = registerBigDecimalField("timeplayed");
  public FieldValue<BigDecimal> timetotal = registerBigDecimalField("timetotal");
  public FieldValue<BigDecimal> percentDone = registerBigDecimalField("percentdone");
  public FieldValue<BigDecimal> remaining = registerBigDecimalField("remaining");
  public FieldValue<BigDecimal> percent = registerBigDecimalField("percent");
  public FieldValue<BigDecimal> percentd = registerBigDecimalField("percentd");
  public FieldValue<BigDecimal> playtime = registerBigDecimalField("playtime");
  public FieldValue<BigDecimal> finalscore = registerBigDecimalField("finalscore");
  public FieldValue<BigDecimal> replay = registerBigDecimalField("replay");
  public FieldValue<BigDecimal> adj = registerBigDecimalField("adj");
  public FieldValue<BigDecimal> remainder = registerBigDecimalField("remainder");


  public FieldValueShort total = registerShortField("total");
  public FieldValueShort totalinc = registerShortField("totalinc");
  public FieldValueShort die1 = registerShortField("die1");
  public FieldValueShort die2 = registerShortField("die2");
  public FieldValueShort finaldie = registerShortField("finaldie");

  public FieldValueInteger steamID = registerIntegerField("steamid");

  public FieldValueString title = registerStringField("title");
  public FieldValueString platform = registerStringField("platform");
  public FieldValueString owned = registerStringField("owned");
  public FieldValueString by = registerStringField("by");
  public FieldValueString reason = registerStringField("reason");
  public FieldValueString icon = registerStringField("icon");
  public FieldValueString logo = registerStringField("logo");
  public FieldValueString metacriticHint = registerStringField("metacritic_hint");
  public FieldValueBoolean metacriticPage = registerBooleanField("metacritic_page");
  public FieldValue<Timestamp> metacriticMatched = registerTimestampField("metacritic_matched");

  public FieldValueBoolean started = registerBooleanField("started");
  public FieldValueBoolean include = registerBooleanField("include");

  @Override
  protected String getTableName() {
    return "games";
  }

  @Override
  public String toString() {
    return title.getValue();
  }
}
