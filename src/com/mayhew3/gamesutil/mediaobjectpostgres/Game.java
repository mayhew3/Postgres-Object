package com.mayhew3.gamesutil.mediaobjectpostgres;

import java.util.Date;

public class Game extends MediaObject {


  public FieldValue<Date> finished = registerDateField("finished");

  public FieldValue<Double> metacritic = registerDoubleField("metacritic");
  public FieldValue<Double> guess = registerDoubleField("guess");
  public FieldValue<Double> mayhew = registerDoubleField("mayhew");
  public FieldValue<Double> sum = registerDoubleField("sum");
  public FieldValue<Double> sumd = registerDoubleField("sumd");
  public FieldValue<Double> hrsPlayed = registerDoubleField("hrs played");
  public FieldValue<Double> hrsTotal = registerDoubleField("hrs total");
  public FieldValue<Double> percentDone = registerDoubleField("percent done");
  public FieldValue<Double> remaining = registerDoubleField("remaining");
  public FieldValue<Double> percent = registerDoubleField("percent");
  public FieldValue<Double> percentd = registerDoubleField("percentd");
  public FieldValue<Double> playtime = registerDoubleField("playtime");
  public FieldValue<Double> finalscore = registerDoubleField("finalscore");
  public FieldValue<Double> replay = registerDoubleField("replay?");
  public FieldValue<Double> adj = registerDoubleField("adj");
  public FieldValue<Double> remainder = registerDoubleField("remainder");


  public FieldValueShort total = registerShortField("total");
  public FieldValueShort totalinc = registerShortField("totalinc");
  public FieldValueShort die1 = registerShortField("die 1");
  public FieldValueShort die2 = registerShortField("die 2");
  public FieldValueShort finaldie = registerShortField("finaldie");

  public FieldValueInteger steamID = registerIntegerField("steamid");

  public FieldValueString game = registerStringField("game");
  public FieldValueString platform = registerStringField("platform");
  public FieldValueString owned = registerStringField("owned");
  public FieldValueString by = registerStringField("by");
  public FieldValueString reason = registerStringField("reason");
  public FieldValueString icon = registerStringField("icon");
  public FieldValueString logo = registerStringField("logo");

  public FieldValueBoolean started = registerBooleanField("started");
  public FieldValueBoolean include = registerBooleanField("include");

  @Override
  protected String getTableName() {
    return "games";
  }

  @Override
  public String toString() {
    return game.getValue();
  }
}
