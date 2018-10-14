package com.mayhew3.mediamogul.model.games;

import com.mayhew3.mediamogul.dataobject.*;

public class PossibleGameMatch extends RetireableDataObject {

  /* FK */
  public FieldValueForeignKey gameId = registerForeignKey(new Game(), Nullability.NOT_NULL);

  /* Data */
  public FieldValueInteger igdbGameExtId = registerIntegerField("igdb_game_ext_id", Nullability.NOT_NULL);
  public FieldValueString igdbGameTitle = registerStringField("igdb_game_title", Nullability.NOT_NULL);
  public FieldValueString poster = registerStringField("poster", Nullability.NULLABLE);

  FieldValueInteger poster_w = registerIntegerField("poster_w", Nullability.NULLABLE);
  FieldValueInteger poster_h = registerIntegerField("poster_h", Nullability.NULLABLE);

  public FieldValueBoolean alreadyExists = registerBooleanField("already_exists", Nullability.NOT_NULL).defaultValue(false);

  public PossibleGameMatch() {
    addUniqueConstraint(gameId, igdbGameExtId);
  }

  @Override
  public String getTableName() {
    return "possible_game_match";
  }

  @Override
  public String toString() {
    return gameId.getValue() + ", Title " + igdbGameTitle.getValue();
  }

}
