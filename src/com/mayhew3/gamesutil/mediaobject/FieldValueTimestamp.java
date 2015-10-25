package com.mayhew3.gamesutil.mediaobject;

import com.sun.istack.internal.Nullable;

import java.sql.Timestamp;
import java.util.Date;

public class FieldValueTimestamp extends FieldValue<Timestamp> {
  public FieldValueTimestamp(String fieldName, FieldConversion<Timestamp> converter) {
    super(fieldName, converter);
  }

  public void changeValue(@Nullable Date date) {
    if (date == null) {
      changeValue(null);
    } else {
      Timestamp timestamp = new Timestamp(date.getTime());
      changeValue(timestamp);
    }
  }
}
