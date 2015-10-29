package com.mayhew3.gamesutil.mediaobject;

public class GenrePostgres extends MediaObjectPostgreSQL {

  /* Data */
  public FieldValueString genreName = registerStringField("name");

  @Override
  protected String getTableName() {
    return "genre";
  }

  @Override
  public String toString() {
    return genreName.getValue();
  }

}
