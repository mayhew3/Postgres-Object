package com.mayhew3.gamesutil.mediaobject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class FieldValueBoolean extends FieldValue<Boolean> {
  private Boolean allowNulls = false;

  public FieldValueBoolean(String fieldName, FieldConversion<Boolean> converter) {
    super(fieldName, converter);
  }
  public FieldValueBoolean(String fieldName, FieldConversion<Boolean> converter, Boolean allowNulls) {
    super(fieldName, converter);
    this.allowNulls = allowNulls;
  }

  @Override
  public void initializeValue(Boolean value) {
    if (allowNulls) {
      super.initializeValue(value);
    } else {
      super.initializeValue((value == null) ? false : value);
    }
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
