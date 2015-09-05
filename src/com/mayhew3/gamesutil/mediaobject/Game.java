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
  public FieldValueInteger giantbomb_id = registerIntegerField("giantbomb_id");
  public FieldValueInteger giantbomb_year = registerIntegerField("giantbomb_year");

  public FieldValueString title = registerStringField("title");
  public FieldValueString platform = registerStringField("platform");
  public FieldValueString owned = registerStringField("owned");
  public FieldValueString by = registerStringField("by");
  public FieldValueString reason = registerStringField("reason");
  public FieldValueString icon = registerStringField("icon");
  public FieldValueString logo = registerStringField("logo");

  public FieldValueString giantbomb_name = registerStringField("giantbomb_name");
  public FieldValueString giantbomb_icon_url = registerStringField("giantbomb_icon_url");
  public FieldValueString giantbomb_medium_url = registerStringField("giantbomb_medium_url");
  public FieldValueString giantbomb_screen_url = registerStringField("giantbomb_screen_url");
  public FieldValueString giantbomb_small_url = registerStringField("giantbomb_small_url");
  public FieldValueString giantbomb_super_url = registerStringField("giantbomb_super_url");
  public FieldValueString giantbomb_thumb_url = registerStringField("giantbomb_thumb_url");
  public FieldValueString giantbomb_tiny_url = registerStringField("giantbomb_tiny_url");
  public FieldValueString giantbomb_best_guess = registerStringField("giantbomb_best_guess");
  public FieldValueString giantbomb_manual_guess = registerStringField("giantbomb_manual_guess");
  public FieldValueBoolean giantbomb_guess_confirmed = registerBooleanField("giantbomb_guess_confirmed");

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
