package com.mayhew3.gamesutil.mediaobject;

import java.sql.PreparedStatement;

public class FieldValueShort extends FieldValue<Short> {
  public FieldValueShort(String fieldName, FieldConversion<Short> converter) {
    super(fieldName, converter);
  }

  @Override
  public void updatePreparedStatement(PreparedStatement preparedStatement, int currentIndex) {
    throw new IllegalStateException("Cannot update Postgres DB with Mongo value.");
  }

  public void increment(Short numberToAdd) {
    Short value = getValue();
    if (value == null) {
      value = 0;
    }
    value = (short) (value + numberToAdd);
    changeValue(value);
  }
}
