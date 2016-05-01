package com.mayhew3.gamesutil.dataobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class FieldValueBoolean extends FieldValue<Boolean> {

  public FieldValueBoolean(String fieldName, FieldConversion<Boolean> converter, Nullability nullability) {
    super(fieldName, converter, nullability);
  }

  public FieldValueBoolean defaultValue(Boolean defaultValue) {
    super.defaultValue(defaultValue);
    return this;
  }

  @Override
  public void initializeValue(Boolean value) {
    if (nullability.equals(Nullability.NULLABLE)) {
      super.initializeValue(value);
    } else {
      super.initializeValue((value == null) ? false : value);
    }
  }

  @Override
  String getDDLType() {
    return "BOOLEAN";
  }

  @Override
  protected void initializeValue(ResultSet resultSet) throws SQLException {
    initializeValue(resultSet.getBoolean(getFieldName()));
  }

  @Override
  public void updatePreparedStatement(PreparedStatement preparedStatement, int currentIndex) throws SQLException {
    if (getChangedValue() == null) {
      preparedStatement.setNull(currentIndex, Types.BOOLEAN);
    } else {
      preparedStatement.setBoolean(currentIndex, getChangedValue());
    }
  }
}
