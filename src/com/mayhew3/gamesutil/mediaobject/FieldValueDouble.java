package com.mayhew3.gamesutil.mediaobject;

import java.sql.PreparedStatement;

public class FieldValueDouble extends FieldValue<Double> {
  public FieldValueDouble(String fieldName, FieldConversion<Double> converter) {
    super(fieldName, converter);
  }

  @Override
  public void updatePreparedStatement(PreparedStatement preparedStatement, int currentIndex) {
    throw new IllegalStateException("Cannot update Postgres DB with Mongo value.");
  }
}
