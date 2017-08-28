package com.mayhew3.mediamogul.dataobject;

import org.jetbrains.annotations.Nullable;

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
  public void initializeValue(@Nullable Boolean value) {
    if (nullability.equals(Nullability.NULLABLE)) {
      super.initializeValue(value);
    } else {
      super.initializeValue((value == null) ? false : value);
    }
  }

  @Override
  public String getDDLType() {
    return "BOOLEAN";
  }

  @Override
  public String getInformationSchemaType() {
    return "boolean";
  }

  @Override
  protected void initializeValue(ResultSet resultSet) throws SQLException {
    Boolean maybeValue = resultSet.getBoolean(getFieldName());
    if (resultSet.wasNull()) {
      maybeValue = null;
    }
    initializeValue(maybeValue);
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
