package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

class EpisodeRating extends DataObject {

  FieldValueForeignKey episodeId = registerForeignKey(new Episode(), Nullability.NOT_NULL);

  FieldValueTimestamp ratingDate = registerTimestampField("rating_date", Nullability.NOT_NULL);

  FieldValueBigDecimal ratingFunny = registerBigDecimalField("rating_funny", Nullability.NULLABLE);
  FieldValueBigDecimal ratingCharacter = registerBigDecimalField("rating_character", Nullability.NULLABLE);
  FieldValueBigDecimal ratingStory = registerBigDecimalField("rating_story", Nullability.NULLABLE);

  FieldValueBigDecimal ratingValue = registerBigDecimalField("rating_value", Nullability.NOT_NULL);

  FieldValueString review = registerStringField("review", Nullability.NULLABLE);

  @Override
  protected String getTableName() {
    return "episode_rating";
  }
}
