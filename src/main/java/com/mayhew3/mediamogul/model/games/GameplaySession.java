package com.mayhew3.mediamogul.model.games;

import com.mayhew3.mediamogul.dataobject.*;

public class GameplaySession extends RetireableDataObject {

  public FieldValueForeignKey gameID = registerForeignKey(new Game(), Nullability.NOT_NULL);

  public FieldValueTimestamp startTime = registerTimestampField("start_time", Nullability.NOT_NULL);

  public FieldValueInteger minutes = registerIntegerField("minutes", Nullability.NOT_NULL);
  public FieldValueInteger rating = registerIntegerField("rating", Nullability.NULLABLE);

  public FieldValueBoolean currentlyPlaying = registerBooleanField("currently_playing", Nullability.NOT_NULL).defaultValue(false);

  @Override
  public String getTableName() {
    return "gameplay_session";
  }
}
