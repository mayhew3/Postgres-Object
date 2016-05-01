package com.mayhew3.gamesutil.dataobject;

import com.sun.istack.internal.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class FieldValueInteger extends FieldValue<Integer> {

  private IntegerSize size = IntegerSize.INTEGER;

  public FieldValueInteger(String fieldName, FieldConversion<Integer> converter, Nullability nullability) {
    super(fieldName, converter, nullability);
  }

  public FieldValueInteger(String fieldName, FieldConversion<Integer> converter, Nullability nullability, IntegerSize size) {
    super(fieldName, converter, nullability);
    this.size = size;
  }

  IntegerSize getSize() {
    return size;
  }

  void setSize(IntegerSize size) {
    this.size = size;
  }

  public FieldValueInteger defaultValue(Integer defaultValue) {
    super.defaultValue(defaultValue);
    return this;
  }

  @Override
  String getDDLType() {
    return size.getDdlIdentifier();
  }

  @Override
  String getInformationSchemaType() {
    return size.getDdlIdentifier();
  }

  @Override
  protected void initializeValue(ResultSet resultSet) throws SQLException {
    Integer resultSetInt = resultSet.getInt(getFieldName());
    if (resultSet.wasNull()) {
      resultSetInt = null;
    }
    initializeValue(resultSetInt);
  }

  /**
   * Used for migrating values from Mongo that were long.
   * @param newValue long value to convert
   */
  public void changeValue(@Nullable Long newValue) {
    if (newValue == null) {
      nullValue();
    } else {
      if (newValue > Integer.MAX_VALUE) {
        throw new IllegalStateException("Cannot convert long that is larger than Integer.MAX_VALUE");
      }
      changeValue(newValue.intValue());
    }
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
