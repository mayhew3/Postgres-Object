package com.mayhew3.gamesutil.mediaobject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class FieldValueInteger extends FieldValue<Integer> {
  public FieldValueInteger(String fieldName, FieldConversion<Integer> converter) {
    super(fieldName, converter);
  }

  @Override
  public void updatePreparedStatement(PreparedStatement preparedStatement, int currentIndex) throws SQLException {
    if (getChangedValue() == null) {
      preparedStatement.setNull(currentIndex, Types.INTEGER);
    } else {
      preparedStatement.setInt(currentIndex, getChangedValue());
    }
  }

  public void increment(Integer numberToAdd) {
    Integer value = getValue();
    if (value == null) {
      value = 0;
    }
    value += numberToAdd;
    changeValue(value);
  }
}
