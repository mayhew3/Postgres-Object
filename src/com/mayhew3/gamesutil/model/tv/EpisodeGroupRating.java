package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

public class EpisodeGroupRating extends DataObject {

  FieldValueForeignKey seriesId = registerForeignKey(new Series(), Nullability.NOT_NULL);

  FieldValueInteger year = registerIntegerField("year", Nullability.NOT_NULL);

  FieldValueTimestamp startDate = registerTimestampField("start_date", Nullability.NOT_NULL);
  FieldValueTimestamp endDate = registerTimestampField("end_date", Nullability.NOT_NULL);

  FieldValueBigDecimal rating = registerBigDecimalField("rating", Nullability.NULLABLE);


  @Override
  protected String getTableName() {
    return "episode_group_rating";
  }
}
