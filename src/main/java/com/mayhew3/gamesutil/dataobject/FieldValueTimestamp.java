package com.mayhew3.gamesutil.dataobject;

import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Date;

public class FieldValueTimestamp extends FieldValue<Timestamp> {
  private Boolean defaultNow = false;
  private OffsetDateTime originalDateTime;
  private OffsetDateTime offsetDateTime;

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
  public String getDDLType() {
    return "TIMESTAMP(6) WITH TIME ZONE";
  }

  @Override
  public String getInformationSchemaType() {
    return "timestamp with time zone";
  }

  @Override
  protected void initializeValue(ResultSet resultSet) throws SQLException {
    initializeValue(resultSet.getTimestamp(getFieldName()));
//    originalDateTime = resultSet.getObject(getFieldName(), OffsetDateTime.class);
//    offsetDateTime = originalDateTime;
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
/*

  public void changeValueWithTimeZone(@Nullable OffsetDateTime offsetDateTime) {
    Preconditions.checkNotNull(offsetDateTime);
    this.offsetDateTime = offsetDateTime;
    Timestamp timestamp = new Timestamp(offsetDateTime.toInstant().toEpochMilli());
    changeValue(timestamp);
  }
*/

  public void changeValueUnlessToNull(@Nullable Date date) {
    if (date != null) {
      changeValue(date);
    }
  }

  public void changeValueFromXMLString(@Nullable String xmlString) {
    if (xmlString != null) {
      long numberOfSeconds = Long.decode(xmlString);
      changeValue(new Timestamp(numberOfSeconds * 1000));
    }
  }
}
