package com.mayhew3.gamesutil.dataobject;

import com.sun.istack.internal.Nullable;

import java.sql.*;
import java.util.Date;

public class FieldValueTimestamp extends FieldValue<Timestamp> {
  private Boolean defaultNow = false;

  public FieldValueTimestamp(String fieldName, FieldConversion<Timestamp> converter, Nullability nullability) {
    super(fieldName, converter, nullability);
  }

  public FieldValueTimestamp defaultValueNow() {
    defaultNow = true;
    return this;
  }

  public String getDefaultValue() {
    if (defaultNow) {
      return "now()";
    }
    return null;
  }

  @Override
  String getDDLType() {
    return "TIMESTAMP(6) WITHOUT TIME ZONE";
  }

  @Override
  String getInformationSchemaType() {
    return "timestamp without time zone";
  }

  @Override
  protected void initializeValue(ResultSet resultSet) throws SQLException {
    initializeValue(resultSet.getTimestamp(getFieldName()));
  }

  @Override
  public void updatePreparedStatement(PreparedStatement preparedStatement, int currentIndex) throws SQLException {
    if (getChangedValue() == null) {
      preparedStatement.setNull(currentIndex, Types.TIMESTAMP);
    } else {
      preparedStatement.setTimestamp(currentIndex, getChangedValue());
    }
  }

  public void changeValue(@Nullable Date date) {
    if (date == null) {
      changeValue(null);
    } else {
      Timestamp timestamp = new Timestamp(date.getTime());
      changeValue(timestamp);
    }
  }

  public void changeValueUnlessToNull(@Nullable Date date) {
    if (date != null) {
      changeValue(date);
    }
  }

  public void changeValueFromXMLString(String xmlString) {
    if (xmlString != null) {
      long numberOfSeconds = Long.decode(xmlString);
      changeValue(new Timestamp(numberOfSeconds * 1000));
    }
  }
}
