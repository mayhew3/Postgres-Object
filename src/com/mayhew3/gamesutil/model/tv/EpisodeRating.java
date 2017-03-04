package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

public class EpisodeRating extends RetireableDataObject {

  public FieldValueForeignKey episodeId = registerForeignKey(new Episode(), Nullability.NOT_NULL);

  public FieldValueTimestamp ratingDate = registerTimestampField("rating_date", Nullability.NOT_NULL);

  public FieldValueBigDecimal ratingFunny = registerBigDecimalField("rating_funny", Nullability.NULLABLE);
  public FieldValueBigDecimal ratingCharacter = registerBigDecimalField("rating_character", Nullability.NULLABLE);
  public FieldValueBigDecimal ratingStory = registerBigDecimalField("rating_story", Nullability.NULLABLE);

  public FieldValueBigDecimal ratingValue = registerBigDecimalField("rating_value", Nullability.NOT_NULL);

  public FieldValueString review = registerStringField("review", Nullability.NULLABLE);

  @Override
  public String getTableName() {
    return "episode_rating";
  }
}
