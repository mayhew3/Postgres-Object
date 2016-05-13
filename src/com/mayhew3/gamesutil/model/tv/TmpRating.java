package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.*;

public class TmpRating extends DataObject {

  FieldValueForeignKey seriesId = registerForeignKey(new Series(), Nullability.NULLABLE);
  FieldValueForeignKey episodeId = registerForeignKey(new Episode(), Nullability.NULLABLE);

  FieldValueString seriesTitle = registerStringField("series_title", Nullability.NOT_NULL);
  FieldValueString episodeTitle = registerStringField("episode_title", Nullability.NULLABLE);
  FieldValueInteger seasonNumber = registerIntegerField("season_number", Nullability.NULLABLE);
  FieldValueInteger episodeNumber = registerIntegerField("episode_number", Nullability.NULLABLE);

  FieldValueTimestamp watchedDate = registerTimestampField("watched_date", Nullability.NULLABLE);

  FieldValueBigDecimal funny = registerBigDecimalField("funny", Nullability.NULLABLE);
  FieldValueBigDecimal character = registerBigDecimalField("character", Nullability.NULLABLE);
  FieldValueBigDecimal story = registerBigDecimalField("story", Nullability.NULLABLE);
  FieldValueBigDecimal rating = registerBigDecimalField("rating", Nullability.NULLABLE);

  FieldValueString note = registerStringField("note", Nullability.NULLABLE);


  @Override
  protected String getTableName() {
    return "tmp_rating";
  }
}
