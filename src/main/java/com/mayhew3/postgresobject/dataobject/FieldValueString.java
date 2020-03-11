package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.DatabaseType;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class FieldValueString extends FieldValue<String> {
  public FieldValueString(String fieldName, FieldConversion<String> converter, Nullability nullability) {
    super(fieldName, converter, nullability);
  }

  @Override
  protected void initializeValue(String value) {
    super.initializeValue(value);
    this.isText = true;
  }

  @Override
  public FieldValueString defaultValue(String defaultValue) {
    super.defaultValue(defaultValue);
    return this;
  }

  @Nullable
  @Override
  public String getInformationSchemaDefault(DatabaseType databaseType) {
    return super.getInformationSchemaDefault(databaseType) == null ? null : super.getInformationSchemaDefault(databaseType) + "::text";
  }

  @Override
  protected void initializeValueFromString(String valueString) {
    super.initializeValueFromString(valueString);
    this.isText = true;
  }

  @Nullable
  @Override
  public String getDefaultValue(DatabaseType databaseType) {
    return super.getDefaultValue(databaseType) == null ? null : "'" + super.getDefaultValue(databaseType) + "'";
  }

  @Override
  public String getDDLType(DatabaseType databaseType) {
    if (databaseType == DatabaseType.POSTGRES) {
      return "TEXT";
    } else {
      return "varchar";
    }
  }

  @Override
  public String getInformationSchemaType(DatabaseType databaseType) {
    if (databaseType == DatabaseType.POSTGRES) {
      return "TEXT";
    } else {
      return "varchar";
    }
  }

  @Override
  protected void initializeValue(ResultSet resultSet) throws SQLException {
    initializeValue(resultSet.getString(getFieldName()));
  }

  @Override
  public void updatePreparedStatement(PreparedStatement preparedStatement, int currentIndex) throws SQLException {
    if (getChangedValue() == null) {
      preparedStatement.setNull(currentIndex, Types.VARCHAR);
    } else {
      preparedStatement.setString(currentIndex, getChangedValue());
    }
  }
}
