package com.mayhew3.mediamogul.model.tv;

import com.mayhew3.mediamogul.dataobject.*;
import com.mayhew3.mediamogul.model.Person;

public class EpisodeRating extends RetireableDataObject {

  public FieldValueForeignKey episodeId = registerForeignKey(new Episode(), Nullability.NOT_NULL);
  public FieldValueForeignKey personId = registerForeignKey(new Person(), Nullability.NOT_NULL);

  public FieldValueBoolean watched = registerBooleanField("watched", Nullability.NOT_NULL).defaultValue(true);
  public FieldValueTimestamp watchedDate = registerTimestampField("watched_date", Nullability.NULLABLE);
  public FieldValueTimestamp ratingDate = registerTimestampField("rating_date", Nullability.NULLABLE);

  public FieldValueBigDecimal ratingFunny = registerBigDecimalField("rating_funny", Nullability.NULLABLE);
  public FieldValueBigDecimal ratingCharacter = registerBigDecimalField("rating_character", Nullability.NULLABLE);
  public FieldValueBigDecimal ratingStory = registerBigDecimalField("rating_story", Nullability.NULLABLE);

  public FieldValueBigDecimal ratingValue = registerBigDecimalField("rating_value", Nullability.NULLABLE);

  public FieldValueString review = registerStringField("review", Nullability.NULLABLE);

  @Override
  public String getTableName() {
    return "episode_rating";
  }
}