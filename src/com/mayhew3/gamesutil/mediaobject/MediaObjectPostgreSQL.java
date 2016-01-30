package com.mayhew3.gamesutil.mediaobject;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.db.PostgresConnection;
import com.sun.javafx.beans.annotations.NonNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class MediaObjectPostgreSQL {

  private enum EditMode {INSERT, UPDATE}

  private EditMode editMode;
  private Boolean initialized = false;

  List<FieldValue> allFieldValues = new ArrayList<>();

  public FieldValueInteger id = new FieldValueInteger("id", new FieldConversionInteger());


  public void initializeFromDBObject(ResultSet resultSet) throws SQLException {
    editMode = EditMode.UPDATE;

    Integer existingId = resultSet.getInt("id");

    if (resultSet.wasNull()) {
      throw new RuntimeException("Row found with no valid id field.");
    }

    initializeValue(id, existingId);

    for (FieldValue fieldValue : allFieldValues) {
      String fieldName = fieldValue.getFieldName();
      Object obj = resultSet.getObject(fieldName);

      initializeValue(fieldValue, obj);
    }

    initialized = true;
  }

  public void initializeForInsert() {
    editMode = EditMode.INSERT;
    initialized = true;
  }

  public void changeToUpdateObject() {
    Preconditions.checkState(initialized, "Shouldn't call change to update object if uninitialized.");
    editMode = EditMode.UPDATE;
  }

  @NonNull
  public boolean isInitialized() {
    return initialized;
  }

  @NonNull
  public Boolean isForInsert() {
    return EditMode.INSERT.equals(editMode);
  }

  @NonNull
  public Boolean isForUpdate() {
    return EditMode.UPDATE.equals(editMode);
  }

  private void initializeValue(FieldValue fieldValue, Object obj) {
    if (obj instanceof String) {
      fieldValue.initializeValueFromString((String) obj);
    } else {
      // todo: Need to change MongoArray fieldvalue to BasicDBList instead of ObjectId[]. Probably need the same for genre.
      fieldValue.initializeValue(obj);
    }
  }

  public void commit(PostgresConnection connection) throws SQLException {
    if (editMode == EditMode.UPDATE) {
      update(connection);
    } else if (editMode == EditMode.INSERT) {
      insert(connection);
    } else {
      throw new IllegalStateException("Attempting to commit MediaObject that wasn't properly initialized!");
    }
  }

  private void insert(PostgresConnection connection) throws SQLException {

    List<FieldValue> changedFields = new ArrayList<>();

    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.getOriginalValue() != null) {
        throw new IllegalStateException("Shouldn't find any original values on Insert object: '" + fieldValue.getFieldName() + "' field has value '"
            + fieldValue.getOriginalValue() + "', changed value '" + fieldValue.getChangedValue() + "'");
      }
      if (fieldValue.isChanged()) {
        changedFields.add(fieldValue);
      }
    }

    if (!changedFields.isEmpty()) {
      Integer resultingID = insertIntoDatabaseAndGetID(connection, changedFields);
      updateObjects(changedFields);
      id.initializeValue(resultingID);
    }

    // INSERT COMPLETE. Subsequent changes should be updates.
    changeToUpdateObject();
  }

  private void update(PostgresConnection db) throws SQLException {
    if (id == null) {
      throw new RuntimeException("Cannot update object with no id field.");
    }
    if (id.isChanged()) {
      throw new RuntimeException("Cannot change id field on existing object.");
    }

    List<String> changedFieldNames = new ArrayList<>();
    List<FieldValue> changedFields = new ArrayList<>();

    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.isChanged()) {
        changedFieldNames.add(fieldValue.getFieldName());
        changedFields.add(fieldValue);
      }
    }
    if (!changedFields.isEmpty()) {
      Joiner joiner = Joiner.on(", ");
      System.out.println(" - Changed: " + joiner.join(changedFieldNames));
      updateDatabase(db, changedFields);
      updateObjects(changedFields);
    }
  }

  private void updateObjects(List<FieldValue> changedFields) {
    for (FieldValue changedField : changedFields) {
      changedField.updateInternal();
    }
  }

  private void updateDatabase(PostgresConnection connection, List<FieldValue> fieldValues) throws SQLException {
    List<String> fieldNames = Lists.newArrayList();

    for (FieldValue fieldValue : fieldValues) {
      fieldNames.add("\"" + fieldValue.getFieldName() + "\" = ?");
    }

    fieldValues.add(id);

    Joiner joiner = Joiner.on(", ");
    String commaSeparatedNames = joiner.join(fieldNames);

    String sql = "UPDATE " + getTableName() + " SET " + commaSeparatedNames + " WHERE ID = ?";

    connection.prepareAndExecuteStatementUpdateWithFields(sql, fieldValues);
  }

  private Integer insertIntoDatabaseAndGetID(PostgresConnection connection, List<FieldValue> fieldValues) throws SQLException {
    List<String> fieldNames = Lists.newArrayList();
    List<String> questionMarks = Lists.newArrayList();

    for (FieldValue fieldValue : fieldValues) {
      fieldNames.add("\"" + fieldValue.getFieldName() + "\"");
      questionMarks.add("?");
    }

    Joiner joiner = Joiner.on(", ");
    String commaSeparatedNames = joiner.join(fieldNames);
    String commaSeparatedQuestionMarks = joiner.join(questionMarks);

    String sql = "INSERT INTO " + getTableName() + " (" + commaSeparatedNames + ") VALUES (" + commaSeparatedQuestionMarks + ")";

    PreparedStatement preparedStatement = connection.getPreparedStatementWithReturnValue(sql);
    connection.executePreparedUpdateWithFields(preparedStatement, fieldValues);

    return getIDFromInsert(preparedStatement);
  }

  private Integer getIDFromInsert(PreparedStatement preparedStatement) {
    try {
      ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

      if (!generatedKeys.next()) {
        throw new RuntimeException("No rows in ResultSet from Inserted object!");
      }

      int id = generatedKeys.getInt("ID");
      preparedStatement.close();
      return id;
    } catch (SQLException e) {
      throw new RuntimeException("Error retrieving ID from inserted object!" + e.getLocalizedMessage());
    }
  }

  protected abstract String getTableName();


  protected final FieldValueBoolean registerBooleanField(String fieldName) {
    FieldValueBoolean fieldBooleanValue = new FieldValueBoolean(fieldName, new FieldConversionBoolean());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValueBoolean registerBooleanFieldAllowingNulls(String fieldName) {
    FieldValueBoolean fieldBooleanValue = new FieldValueBoolean(fieldName, new FieldConversionBoolean(), Boolean.TRUE);
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValueTimestamp registerTimestampField(String fieldName) {
    FieldValueTimestamp fieldTimestampValue = new FieldValueTimestamp(fieldName, new FieldConversionTimestamp());
    allFieldValues.add(fieldTimestampValue);
    return fieldTimestampValue;
  }

  protected final FieldValueInteger registerIntegerField(String fieldName) {
    FieldValueInteger fieldIntegerValue = new FieldValueInteger(fieldName, new FieldConversionInteger());
    allFieldValues.add(fieldIntegerValue);
    return fieldIntegerValue;
  }

  protected final FieldValueBigDecimal registerBigDecimalField(String fieldName) {
    FieldValueBigDecimal fieldBigDecimalValue = new FieldValueBigDecimal(fieldName, new FieldConversionBigDecimal());
    allFieldValues.add(fieldBigDecimalValue);
    return fieldBigDecimalValue;
  }

  protected final FieldValueString registerStringField(String fieldName) {
    FieldValueString fieldBooleanValue = new FieldValueString(fieldName, new FieldConversionString());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

}
