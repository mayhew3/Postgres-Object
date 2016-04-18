package com.mayhew3.gamesutil.dataobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class FieldValueLong extends FieldValue<Long> {
  public FieldValueLong(String fieldName, FieldConversion<Long> converter) {
    super(fieldName, converter);
  }

  @Override
  protected void initializeValue(ResultSet resultSet) throws SQLException {
    Long resultSetInt = resultSet.getLong(getFieldName());
    if (resultSet.wasNull()) {
      resultSetInt = null;
    }
    initializeValue(resultSetInt);
  }

  @Override
  public void updatePreparedStatement(PreparedStatement preparedStatement, int currentIndex) throws SQLException {
    if (getChangedValue() == null) {
      preparedStatement.setNull(currentIndex, Types.INTEGER);
    } else {
      preparedStatement.setLong(currentIndex, getChangedValue());
    }
  }

}
