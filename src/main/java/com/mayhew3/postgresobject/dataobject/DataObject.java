package com.mayhew3.postgresobject.dataobject;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.mayhew3.postgresobject.db.DatabaseType;
import com.mayhew3.postgresobject.db.SQLConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"SameParameterValue", "unused", "rawtypes"})
public abstract class DataObject {

  private enum EditMode {INSERT, UPDATE}

  private EditMode editMode;
  private Boolean initialized = false;

  private List<FieldValue> allFieldValues = new ArrayList<>();
  private List<UniqueConstraint> uniqueConstraints = new ArrayList<>();
  private List<ColumnsIndex> indices = new ArrayList<>();
  private List<FieldValueForeignKey> foreignKeys = new ArrayList<>();
  private List<String> sequenceNames = new ArrayList<>();

  public FieldValueSerial id = registerId();

  public FieldValueTimestamp dateAdded = registerTimestampField("date_added", Nullability.NULLABLE).defaultValueNow();

  private static Logger logger = LogManager.getLogger(DataObject.class);

  public DataObject() {
    super();
    addUniqueConstraint(id);
  }

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

  public List<FieldValue> getChangedFields() {
    return allFieldValues.stream().filter(FieldValue::isChanged).collect(Collectors.toList());
  }

  public List<FieldValue> getAllFieldValues() {
    return Lists.newArrayList(allFieldValues);
  }

  public List<FieldValue> getAllFieldValuesIncludingId() {
    ArrayList<FieldValue> fieldValues = Lists.newArrayList(allFieldValues);
    fieldValues.add(id);
    return fieldValues;
  }

  List<FieldValueForeignKey> getForeignKeys() {
    return Lists.newArrayList(foreignKeys);
  }

  List<ColumnsIndex> getIndices() {
    return Lists.newArrayList(indices);
  }

  List<UniqueConstraint> getUniqueIndices() {
    return Lists.newArrayList(uniqueConstraints);
  }

  void preInsert() {
    // nothing by default
  }

  @NotNull
  public Boolean isInitialized() {
    return initialized;
  }

  @NotNull
  public Boolean isForInsert() {
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
      preInsert();
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
      debug(" - Changed: " + joiner.join(changedFieldNames));
      updateDatabase(db, changedFields);
      updateObjects(changedFields);
    }
  }

  @Nullable
  public FieldValue getFieldValueWithName(String fieldName) {
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

  private void updateObjects(List<FieldValue> changedFields) {
    changedFields.forEach(FieldValue::updateInternal);
  }

  private void updateDatabase(SQLConnection connection, List<FieldValue> fieldValues) throws SQLException {
    List<String> fieldNames = Lists.newArrayList();

    fieldNames.addAll(fieldValues
        .stream()
        .map(fieldValue -> fieldValue.getFieldNameDBSafe(connection.getDatabaseType()) + " = ?")
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
      fieldNames.add(fieldValue.getFieldNameDBSafe(connection.getDatabaseType()));
      questionMarks.add("?");
    }

    Joiner joiner = Joiner.on(", ");
    String commaSeparatedNames = joiner.join(fieldNames);
    String commaSeparatedQuestionMarks = joiner.join(questionMarks);

    String sql = "INSERT INTO " + getTableName() + " (" + commaSeparatedNames + ") VALUES (" + commaSeparatedQuestionMarks + ")";

    return connection.prepareAndExecuteStatementInsertReturnId(sql, fieldValues);
  }

  protected void addUniqueConstraint(FieldValue... fieldValues) {
    uniqueConstraints.add(new UniqueConstraint(Lists.newArrayList(fieldValues), getTableName()));
  }

  @SuppressWarnings("SameParameterValue")
  protected void addColumnsIndex(FieldValue... fieldValues) {
    indices.add(new ColumnsIndex(Lists.newArrayList(fieldValues), getTableName()));
  }

  public String generateTableCreateStatement(DatabaseType databaseType) {
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
      String statementPiece = fieldValue.getFieldName() + " " + fieldValue.getDDLType(databaseType);
      if (fieldValue.getDefaultValue(databaseType) != null) {
        statementPiece += " DEFAULT " + fieldValue.getDefaultValue(databaseType);
      }
      if (!fieldValue.nullability.getAllowNulls()) {
        statementPiece += " NOT NULL";
      }
      statementPieces.add(statementPiece);
    }

    statementPieces.add("PRIMARY KEY (" + id.getFieldName() + ")");

    for (UniqueConstraint index : uniqueConstraints) {
      List<String> fieldNames = index.getFields().stream().map(FieldValue::getFieldName).collect(Collectors.toList());
      String join = Joiner.on(", ").join(fieldNames);
      if (!"id".equals(join)) {
        statementPieces.add("UNIQUE (" + join + ")");
      }
    }

    String allFieldDeclarations = Joiner.on(", ").join(statementPieces);

    statement += allFieldDeclarations;
    statement += ")";

    return statement;
  }

  public List<String> generateAddForeignKeyStatements() {
    List<String> statements = new ArrayList<>();
    int fkIndex = 1;
    for (FieldValueForeignKey foreignKey : foreignKeys) {
      String constraintName = getTableName() + "_fk" + fkIndex;
      String statement =
          "ALTER TABLE " + getTableName() + " " +
              "ADD CONSTRAINT " + constraintName + " " +
              "FOREIGN KEY (" + foreignKey.getFieldName() + ") " +
              "REFERENCES " + foreignKey.getTableName() + " (id)";
      statements.add(statement);

      fkIndex++;
    }
    return statements;
  }

  public List<String> generateAddIndexStatements() {
    List<String> statements = new ArrayList<>();
    for (ColumnsIndex index : indices) {
      List<String> fieldNames = index.getFields().stream().map(FieldValue::getFieldName).collect(Collectors.toList());
      String commaJoin = Joiner.on(", ").join(fieldNames);
      String indexName = index.getIndexName();
      String statement =
          "CREATE INDEX " + indexName + " " +
              "ON " + getTableName() + " " +
              "(" + commaJoin + ") ";
      statements.add(statement);
    }
    return statements;
  }

  public List<String> generateAddUniqueIndexStatements() {
    List<String> statements = new ArrayList<>();
    for (UniqueConstraint index : uniqueConstraints) {
      List<String> fieldNames = index.getFields().stream().map(FieldValue::getFieldName).collect(Collectors.toList());
      String commaJoin = Joiner.on(", ").join(fieldNames);
      if (!"id".equals(commaJoin)) {
        String indexName = index.getIndexName();
        String statement =
            "CREATE UNIQUE INDEX " + indexName + " " +
                "ON " + getTableName() + " " +
                "(" + commaJoin + ") ";
        statements.add(statement);
      }
    }
    return statements;
  }

  List<String> getSequenceNames() {
    return Lists.newArrayList(sequenceNames);
  }

  private String getSerialDDLType() {
    IntegerSize size = id.getSize();
    if (size.equals(IntegerSize.BIGINT)) {
      return "bigserial";
    } else {
      return "serial";
    }
  }

  public abstract String getTableName();

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

  protected final FieldValueDate registerDateField(String fieldName, Nullability nullability) {
    FieldValueDate fieldDateValue = new FieldValueDate(fieldName, new FieldConversionDate(), nullability);
    allFieldValues.add(fieldDateValue);
    return fieldDateValue;
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

  protected final FieldValueSerial registerSerialField(String fieldName) {
    String sequenceName = getTableName() + "_" + fieldName + "_seq";
    FieldValueSerial fieldSerialValue = new FieldValueSerial(fieldName, new FieldConversionInteger(), Nullability.NOT_NULL, sequenceName);
    sequenceNames.add(sequenceName);
    allFieldValues.add(fieldSerialValue);
    return fieldSerialValue;
  }

  private FieldValueSerial registerId() {
    String sequenceName = getTableName() + "_id_seq";
    FieldValueSerial fieldIntegerValue = new FieldValueSerial("id", new FieldConversionInteger(), Nullability.NOT_NULL, sequenceName);
    sequenceNames.add(sequenceName);
    return fieldIntegerValue;
  }

  protected final FieldValueForeignKey registerForeignKey(DataObject dataObject, Nullability nullability) {
    String fieldName = dataObject.getTableName() + "_id";
    FieldValueForeignKey fieldValueForeignKey = new FieldValueForeignKey(fieldName, new FieldConversionInteger(), nullability, dataObject);
    allFieldValues.add(fieldValueForeignKey);
    foreignKeys.add(fieldValueForeignKey);
    return fieldValueForeignKey;
  }

  protected final FieldValueForeignKey registerForeignKeyWithName(DataObject dataObject, Nullability nullability, String columnName) {
    FieldValueForeignKey fieldValueForeignKey = new FieldValueForeignKey(columnName, new FieldConversionInteger(), nullability, dataObject);
    allFieldValues.add(fieldValueForeignKey);
    foreignKeys.add(fieldValueForeignKey);
    return fieldValueForeignKey;
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

  void debug(Object message) {
    logger.debug(message);
  }
}
