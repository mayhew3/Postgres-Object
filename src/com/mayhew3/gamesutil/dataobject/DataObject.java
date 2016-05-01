package com.mayhew3.gamesutil.dataobject;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public abstract class DataObject {

  private enum EditMode {INSERT, UPDATE}

  private EditMode editMode;
  private Boolean initialized = false;

  private List<FieldValue> allFieldValues = new ArrayList<>();
  private List<UniqueConstraint> indices = new ArrayList<>();

  public FieldValueSerial id = new FieldValueSerial("id", new FieldConversionInteger(), Nullability.NOT_NULL, (getTableName() + "_id_seq"));

  public FieldValueTimestamp dateAdded = registerTimestampField("date_added", Nullability.NULLABLE).defaultValueNow();

  public void initializeFromDBObject(ResultSet resultSet) throws SQLException {
    editMode = EditMode.UPDATE;

    Integer existingId = resultSet.getInt("id");

    if (resultSet.wasNull()) {
      throw new RuntimeException("Row found with no valid id field.");
    }

    id.initializeValue(existingId);

    for (FieldValue fieldValue : allFieldValues) {
      fieldValue.initializeValue(resultSet);
    }

    initialized = true;
  }

  public void initializeForInsert() {
    editMode = EditMode.INSERT;
    initialized = true;
  }

  void changeToUpdateObject() {
    Preconditions.checkState(initialized, "Shouldn't call change to update object if uninitialized.");
    editMode = EditMode.UPDATE;
  }

  public Boolean hasChanged() {
    return !isForInsert() && !getChangedFields().isEmpty();
  }

  private List<FieldValue> getChangedFields() {
    return allFieldValues.stream().filter(FieldValue::isChanged).collect(Collectors.toList());
  }

  List<FieldValue> getAllFieldValues() {
    return Lists.newArrayList(allFieldValues);
  }

  @NotNull
  boolean isInitialized() {
    return initialized;
  }

  @NotNull
  Boolean isForInsert() {
    return EditMode.INSERT.equals(editMode);
  }

  @NotNull
  Boolean isForUpdate() {
    return EditMode.UPDATE.equals(editMode);
  }

  public void commit(SQLConnection connection) throws SQLException {
    if (editMode == EditMode.UPDATE) {
      update(connection);
    } else if (editMode == EditMode.INSERT) {
      insert(connection);
    } else {
      throw new IllegalStateException("Attempting to commit MediaObject that wasn't properly initialized!");
    }
  }

  private void insert(SQLConnection connection) throws SQLException {

    initializeDateAdded();

    List<FieldValue> changedFields = new ArrayList<>();

    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.getOriginalValue() != null) {
        throw new IllegalStateException("Shouldn't find any original values on Insert object: '" + fieldValue.getFieldName() + "' field has value '"
            + fieldValue.getOriginalValue() + "', changed value '" + fieldValue.getChangedValue() + "'");
      }
      if (fieldValue.isChanged() || fieldValue.getExplicitNull()) {
        changedFields.add(fieldValue);
      }
    }

    Integer resultingID = insertIntoDatabaseAndGetID(connection, changedFields);
    updateObjects(changedFields);
    id.initializeValue(resultingID);

    // INSERT COMPLETE. Subsequent changes should be updates.
    changeToUpdateObject();
  }


  private void initializeDateAdded() {
    if (dateAdded.getValue() == null) {
      dateAdded.changeValue(new Date());
    }
  }

  private void update(SQLConnection db) throws SQLException {
    if (id == null) {
      throw new RuntimeException("Cannot update object with no id field.");
    }
    if (id.isChanged()) {
      throw new RuntimeException("Cannot change id field on existing object.");
    }

    List<String> changedFieldNames = new ArrayList<>();
    List<FieldValue> changedFields = new ArrayList<>();

    allFieldValues.stream().filter(FieldValue::isChanged).forEach(fieldValue -> {
      changedFieldNames.add(fieldValue.getFieldName());
      changedFields.add(fieldValue);
    });

    if (!changedFields.isEmpty()) {
      Joiner joiner = Joiner.on(", ");
      System.out.println(" - Changed: " + joiner.join(changedFieldNames));
      updateDatabase(db, changedFields);
      updateObjects(changedFields);
    }
  }

  @Nullable
  FieldValue getFieldValueWithName(String fieldName) {
    if ("id".equals(fieldName)) {
      return id;
    }
    List<FieldValue> fieldValues = allFieldValues.stream().filter(f -> f.getFieldName().equals(fieldName)).collect(Collectors.toList());
    if (fieldValues.size() > 1) {
      throw new IllegalStateException("Found multiple field values with name '" + fieldName + "'.");
    } else if (fieldValues.isEmpty()) {
      return null;
    } else {
      return fieldValues.get(0);
    }
  }

  protected void changeIdIntegerSize(IntegerSize integerSize) {
    this.id.setSize(integerSize);
  }

  private void updateObjects(List<FieldValue> changedFields) {
    changedFields.forEach(FieldValue::updateInternal);
  }

  private void updateDatabase(SQLConnection connection, List<FieldValue> fieldValues) throws SQLException {
    List<String> fieldNames = Lists.newArrayList();

    fieldNames.addAll(fieldValues
        .stream()
        .map(fieldValue -> "\"" + fieldValue.getFieldName() + "\" = ?")
        .collect(Collectors.toList()));

    fieldValues.add(id);

    Joiner joiner = Joiner.on(", ");
    String commaSeparatedNames = joiner.join(fieldNames);

    String sql = "UPDATE " + getTableName() + " SET " + commaSeparatedNames + " WHERE ID = ?";

    connection.prepareAndExecuteStatementUpdateWithFields(sql, fieldValues);
  }

  private Integer insertIntoDatabaseAndGetID(SQLConnection connection, List<FieldValue> fieldValues) throws SQLException {
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

    return connection.prepareAndExecuteStatementInsertReturnId(sql, fieldValues);
  }

  protected void addUniqueConstraint(FieldValue... fieldValues) {
    indices.add(new UniqueConstraint(Lists.newArrayList(fieldValues)));
  }

  String generateTableCreateStatement() {
    String statement =
        "CREATE TABLE " + getTableName() +
            " (";

    List<String> statementPieces = new ArrayList<>();

    String idPiece = id.getFieldName() + " " + getSerialDDLType();
    if (!id.nullability.getAllowNulls()) {
      idPiece += " NOT NULL";
    }
    statementPieces.add(idPiece);

    for (FieldValue fieldValue : allFieldValues) {
      String statementPiece = fieldValue.getFieldName() + " " + fieldValue.getDDLType();
      if (fieldValue.getDefaultValue() != null) {
        statementPiece += " DEFAULT " + fieldValue.getDefaultValue();
      }
      if (!fieldValue.nullability.getAllowNulls()) {
        statementPiece += " NOT NULL";
      }
      statementPieces.add(statementPiece);
    }

    statementPieces.add("PRIMARY KEY (" + id.getFieldName() + ")");

    for (UniqueConstraint index : indices) {
      List<String> fieldNames = index.getFields().stream().map(FieldValue::getFieldName).collect(Collectors.toList());
      String join = Joiner.on(", ").join(fieldNames);
      statementPieces.add("UNIQUE (" + join + ")");
    }

    String allFieldDeclarations = Joiner.on(", ").join(statementPieces);

    statement += allFieldDeclarations;
    statement += ")";

    return statement;
  }

  private String getSerialDDLType() {
    IntegerSize size = id.getSize();
    if (size.equals(IntegerSize.BIGINT)) {
      return "bigserial";
    } else {
      return "serial";
    }
  }

  protected abstract String getTableName();

  // todo: make abstract, and force all subtypes to implement.
  protected String createDDLStatement() {
    throw new UnsupportedOperationException("This method needs to be implemented on all subtypes that call it.");
  }

  protected final FieldValueBoolean registerBooleanField(String fieldName, Nullability nullability) {
    FieldValueBoolean fieldBooleanValue = new FieldValueBoolean(fieldName, new FieldConversionBoolean(), nullability);
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValueBoolean registerBooleanFieldAllowingNulls(String fieldName, Nullability nullability) {
    FieldValueBoolean fieldBooleanValue = new FieldValueBoolean(fieldName, new FieldConversionBoolean(), nullability);
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValueTimestamp registerTimestampField(String fieldName, Nullability nullability) {
    FieldValueTimestamp fieldTimestampValue = new FieldValueTimestamp(fieldName, new FieldConversionTimestamp(), nullability);
    allFieldValues.add(fieldTimestampValue);
    return fieldTimestampValue;
  }

  protected final FieldValueInteger registerIntegerField(String fieldName, Nullability nullability) {
    FieldValueInteger fieldIntegerValue = new FieldValueInteger(fieldName, new FieldConversionInteger(), nullability);
    allFieldValues.add(fieldIntegerValue);
    return fieldIntegerValue;
  }

  protected final FieldValueInteger registerIntegerField(String fieldName, Nullability nullability, IntegerSize integerSize) {
    FieldValueInteger fieldIntegerValue = new FieldValueInteger(fieldName, new FieldConversionInteger(), nullability, integerSize);
    allFieldValues.add(fieldIntegerValue);
    return fieldIntegerValue;
  }

  protected final FieldValueBigDecimal registerBigDecimalField(String fieldName, Nullability nullability) {
    FieldValueBigDecimal fieldBigDecimalValue = new FieldValueBigDecimal(fieldName, new FieldConversionBigDecimal(), nullability);
    allFieldValues.add(fieldBigDecimalValue);
    return fieldBigDecimalValue;
  }

  protected final FieldValueString registerStringField(String fieldName, Nullability nullability) {
    FieldValueString fieldBooleanValue = new FieldValueString(fieldName, new FieldConversionString(), nullability);
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

}
