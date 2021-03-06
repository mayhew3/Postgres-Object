package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.DatabaseType;
import org.jetbrains.annotations.NotNull;
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
  public String getDefaultValue(DatabaseType databaseType) {
    if (databaseType == DatabaseType.POSTGRES) {
      return super.getDefaultValue(databaseType);
    } else if (databaseType == DatabaseType.MYSQL) {
      return defaultValue ? "1" : "0";
    } else {
      throw new IllegalStateException("Only PostgreSQL and MySQL supported.");
    }
  }

  @Override
  public String getDDLType(DatabaseType databaseType) {
    return getDataType(databaseType);
  }

  @Override
  public String getInformationSchemaType(DatabaseType databaseType) {
    return getDataType(databaseType);
  }

  @NotNull
  private String getDataType(DatabaseType databaseType) {
    if (databaseType == DatabaseType.POSTGRES) {
      return "BOOLEAN";
    } else if (databaseType == DatabaseType.MYSQL) {
      return "tinyint";
    } else {
      throw new IllegalStateException("Only PostgreSQL and MySQL supported.");
    }
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
