package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.DatabaseType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

public class FieldValueDate extends FieldValue<Date> {
  public FieldValueDate(String fieldName, FieldConversion<Date> converter, Nullability nullability) {
    super(fieldName, converter, nullability);
  }

  @Override
  public FieldValueDate defaultValue(Date defaultValue) {
    super.defaultValue(defaultValue);
    return this;
  }

  @Override
  public String getDDLType(DatabaseType databaseType) {
    return "DATE";
  }

  @Override
  public String getInformationSchemaType(DatabaseType databaseType) {
    return "date";
  }

  @Override
  protected void initializeValue(ResultSet resultSet) throws SQLException {
    initializeValue(resultSet.getDate(getFieldName()));
  }

  @Override
  public void updatePreparedStatement(PreparedStatement preparedStatement, int currentIndex) throws SQLException {
    if (getChangedValue() == null) {
      preparedStatement.setNull(currentIndex, Types.DATE);
    } else {
      java.sql.Date sqlDate = new java.sql.Date(getChangedValue().getTime());
      preparedStatement.setDate(currentIndex, sqlDate);
    }
  }

  public void changeValueFromXMLString(String xmlString) {
    if (xmlString != null) {
      long numberOfSeconds = Long.decode(xmlString);
      changeValue(new Date(numberOfSeconds * 1000));
    }
  }
}
