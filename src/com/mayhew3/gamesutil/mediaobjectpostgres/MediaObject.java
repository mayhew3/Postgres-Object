package com.mayhew3.gamesutil.mediaobjectpostgres;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.games.PostgresConnection;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public abstract class MediaObject {

  private enum EditMode {INSERT, UPDATE}

  private EditMode editMode;

  List<FieldValue> allFieldValues = new ArrayList<>();

  public FieldValue<Integer> id = new FieldValue<>("id", new FieldConversionInteger());


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
  }

  public void initializeForInsert() {
    editMode = EditMode.INSERT;
  }

  public void changeToUpdateObject() {
    editMode = EditMode.UPDATE;
  }

  private void initializeValue(FieldValue fieldValue, Object obj) {
    if (obj instanceof String) {
      fieldValue.initializeValueFromString((String) obj);
    } else {
      // todo: Need to change MongoArray fieldvalue to BasicDBList instead of ObjectId[]. Probably need the same for genre.
      fieldValue.initializeValue(obj);
    }
  }

  public void commit(PostgresConnection connection) {
    if (editMode == EditMode.UPDATE) {
      update(connection);
    } else if (editMode == EditMode.INSERT) {
      insert(connection);
    } else {
      throw new IllegalStateException("Attempting to commit MediaObject that wasn't properly initialized!");
    }
  }

  private void insert(PostgresConnection connection) {
    BasicDBObject insertObject = new BasicDBObject();

    List<FieldValue> changedFields = new ArrayList<>();

    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.getOriginalValue() != null) {
        throw new IllegalStateException("Shouldn't find any original values on Insert object: '" + fieldValue.getFieldName() + "' field has value '"
            + fieldValue.getOriginalValue() + "', changed value '" + fieldValue.getChangedValue() + "'");
      }
      if (fieldValue.isChanged()) {
        insertObject.append(fieldValue.getFieldName(), fieldValue.getChangedValue());
        changedFields.add(fieldValue);
      }
    }

    if (!insertObject.isEmpty()) {
      insertIntoDatabase(connection, insertObject);
      updateObjects(changedFields);
      id.initializeValue((Integer) insertObject.get("id"));
    }

    // INSERT COMPLETE. Subsequent changes should be updates.
    editMode = EditMode.UPDATE;
  }

  private void update(PostgresConnection db) {
    if (id == null) {
      throw new RuntimeException("Cannot update object with no id field.");
    }
    if (id.isChanged()) {
      throw new RuntimeException("Cannot change id field on existing object.");
    }
    BasicDBObject updateObject = new BasicDBObject();

    List<FieldValue> changedFields = new ArrayList<>();

    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.isChanged()) {
        updateObject.append(fieldValue.getFieldName(), fieldValue.getChangedValue());
        changedFields.add(fieldValue);
      }
    }
    if (!updateObject.isEmpty()) {
      Joiner joiner = Joiner.on(", ");
      System.out.println(" - Changed: " + joiner.join(updateObject.keySet()));
      updateDatabase(db, updateObject);
      updateObjects(changedFields);
    }
  }

  private void updateObjects(List<FieldValue> changedFields) {
    for (FieldValue changedField : changedFields) {
      changedField.updateInternal();
    }
  }

  private void updateDatabase(PostgresConnection connection, BasicDBObject updateObject) {
    List<String> fieldNames = Lists.newArrayList();
    List<Object> fieldValues = Lists.newArrayList();

    for (String fieldName : updateObject.keySet()) {
      fieldNames.add("\"" + fieldName + "\" = ?");
      fieldValues.add(updateObject.get(fieldName));
    }

    fieldValues.add(id.getValue());

    Joiner joiner = Joiner.on(", ");
    String commaSeparatedNames = joiner.join(fieldNames);

    String sql = "UPDATE " + getTableName() + " SET " + commaSeparatedNames + " WHERE ID = ?";

    connection.prepareAndExecuteStatementUpdate(sql, fieldValues);
  }

  private void insertIntoDatabase(PostgresConnection connection, BasicDBObject updateObject) {
    List<String> fieldNames = Lists.newArrayList();
    List<String> questionMarks = Lists.newArrayList();
    List<Object> fieldValues = Lists.newArrayList();

    for (String fieldName : updateObject.keySet()) {
      fieldNames.add("\"" + fieldName + "\"");
      questionMarks.add("?");
      fieldValues.add(updateObject.get(fieldName));
    }

    Joiner joiner = Joiner.on(", ");
    String commaSeparatedNames = joiner.join(fieldNames);
    String commaSeparatedQuestionMarks = joiner.join(questionMarks);

    String sql = "INSERT INTO " + getTableName() + " (" + commaSeparatedNames + ") VALUES (" + commaSeparatedQuestionMarks + ")";

    connection.prepareAndExecuteStatementUpdate(sql, fieldValues);
  }

  public void markFieldsForUpgrade() {
    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.shouldUpgradeText()) {
        fieldValue.changeValue(fieldValue.getOriginalValue());
      }
    }
  }

  protected abstract String getTableName();

  public List<FieldValue> getAllFieldValues() {
    return allFieldValues;
  }

  public FieldValue getMatchingField(FieldValue matchingValue) {
    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.getFieldName().equals(matchingValue.getFieldName())) {
        return fieldValue;
      }
    }
    return null;
  }


  protected final FieldValue<BasicDBList> registerStringArrayField(String fieldName) {
    FieldValue<BasicDBList> fieldStringArrayValue = new FieldValue<>(fieldName, new FieldConversionStringArray());
    allFieldValues.add(fieldStringArrayValue);
    return fieldStringArrayValue;
  }

  protected final FieldValueDBObjectArray registerIntegerDBObjectArrayField(String fieldName) {
    FieldValueDBObjectArray fieldIntegerArrayValue = new FieldValueDBObjectArray(fieldName, new FieldConversionIntegerDBObjectArray());
    allFieldValues.add(fieldIntegerArrayValue);
    return fieldIntegerArrayValue;
  }

  protected final FieldValueBoolean registerBooleanField(String fieldName) {
    FieldValueBoolean fieldBooleanValue = new FieldValueBoolean(fieldName, new FieldConversionBoolean());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValueDate registerDateField(String fieldName) {
    FieldValueDate fieldBooleanValue = new FieldValueDate(fieldName, new FieldConversionDate());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValue<Timestamp> registerTimestampField(String fieldName) {
    FieldValue<Timestamp> fieldTimestampValue = new FieldValue<>(fieldName, new FieldConversionTimestamp());
    allFieldValues.add(fieldTimestampValue);
    return fieldTimestampValue;
  }

  protected final FieldValueInteger registerIntegerField(String fieldName) {
    FieldValueInteger fieldIntegerValue = new FieldValueInteger(fieldName, new FieldConversionInteger());
    allFieldValues.add(fieldIntegerValue);
    return fieldIntegerValue;
  }

  protected final FieldValueShort registerShortField(String fieldName) {
    FieldValueShort fieldShortValue = new FieldValueShort(fieldName, new FieldConversionShort());
    allFieldValues.add(fieldShortValue);
    return fieldShortValue;
  }

  protected final FieldValue<Double> registerDoubleField(String fieldName) {
    FieldValue<Double> fieldDoubleValue = new FieldValue<>(fieldName, new FieldConversionDouble());
    allFieldValues.add(fieldDoubleValue);
    return fieldDoubleValue;
  }

  protected final FieldValue<BigDecimal> registerBigDecimalField(String fieldName) {
    FieldValue<BigDecimal> fieldBigDecimalValue = new FieldValue<>(fieldName, new FieldConversionBigDecimal());
    allFieldValues.add(fieldBigDecimalValue);
    return fieldBigDecimalValue;
  }

  protected final FieldValueString registerStringField(String fieldName) {
    FieldValueString fieldBooleanValue = new FieldValueString(fieldName, new FieldConversionString());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

}
