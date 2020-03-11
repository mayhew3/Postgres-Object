package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.SQLConnection;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class FieldValueBigDecimal extends FieldValue<BigDecimal> {
  public FieldValueBigDecimal(String fieldName, FieldConversion<BigDecimal> converter, Nullability nullability) {
    super(fieldName, converter, nullability);
  }

  @Override
  public String getDDLType(SQLConnection connection) {
    return "NUMERIC";
  }

  @Override
  public String getInformationSchemaType(SQLConnection connection) {
    return "numeric";
  }

  @Override
  protected void initializeValue(ResultSet resultSet) throws SQLException {
    initializeValue(resultSet.getBigDecimal(getFieldName()));
  }

  @Override
  public void updatePreparedStatement(PreparedStatement preparedStatement, int currentIndex) throws SQLException {
    if (getChangedValue() == null) {
      preparedStatement.setNull(currentIndex, Types.NUMERIC);
    } else {
      preparedStatement.setBigDecimal(currentIndex, getChangedValue());
    }
  }

  public void changeValue(@Nullable Double newValue) {
    if (newValue == null) {
      changeValue((BigDecimal) null);
    } else {
      changeValue(BigDecimal.valueOf(newValue));
    }
  }
}
