package com.mayhew3.mediamogul.dataobject;

import com.mayhew3.mediamogul.db.SQLConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class DataObjectTableValidator {
  private DataObject dataObject;
  private SQLConnection connection;

  private List<DataObjectMismatch> mismatches;

  DataObjectTableValidator(DataObject dataObject, SQLConnection connection) {
    this.dataObject = dataObject;
    this.connection = connection;

    mismatches = new ArrayList<>();
  }

  List<DataObjectMismatch> matchSchema() throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT COUNT(1) as num_tables " +
            "FROM information_schema.tables " +
            "WHERE table_schema = ? " +
            "AND table_name = ? ",
        "public", dataObject.getTableName()
    );
    resultSet.next();
    if (resultSet.getInt("num_tables") != 1) {
      addMismatch("Table not found!");
      return mismatches;
    }

    matchFields();
    matchForeignKeys();

    return mismatches;
  }

  private void matchFields() throws SQLException {
    List<FieldValue> unfoundFieldValues = dataObject.getAllFieldValues();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM information_schema.columns " +
            "WHERE table_schema = ? " +
            "AND table_name = ? ",
        "public", dataObject.getTableName()
    );

    while (resultSet.next()) {
      String column_name = resultSet.getString("column_name");
      FieldValue fieldValue = dataObject.getFieldValueWithName(column_name);

      if (fieldValue == null) {
        addMismatch("DB column '" + column_name + "' specified in DB, but not found.");
      } else {
        String column_default = resultSet.getString("column_default");
        Boolean is_nullable = resultSet.getString("is_nullable").equals("YES");
        String data_type = resultSet.getString("data_type");

        if (!matchesIgnoreCase(column_default, fieldValue.getInformationSchemaDefault())) {
          addMismatch(fieldValue, "DEFAULT mismatch: DB value: " + column_default + ", Field value: " + fieldValue.getInformationSchemaDefault());
        }
        if (!is_nullable.equals(fieldValue.nullability.getAllowNulls())) {
          addMismatch(fieldValue, "is_nullable mismatch: DB value: " + is_nullable + ", Field value: " + fieldValue.nullability.getAllowNulls());
        }
        if (!matchesIgnoreCase(data_type, fieldValue.getInformationSchemaType())) {
          addMismatch(fieldValue, "data_type mismatch: DB value: " + data_type + ", Field value: " + fieldValue.getDDLType());
        }

        unfoundFieldValues.remove(fieldValue);
      }
    }

    if (!unfoundFieldValues.isEmpty()) {
      for (FieldValue fieldValue : unfoundFieldValues) {
        addMismatch(fieldValue, "FieldValue not found in DB.");
      }
    }
  }

  private void matchForeignKeys() throws SQLException {
    List<FieldValueForeignKey> unfoundForeignKeys = dataObject.getForeignKeys();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT " +
            "  tc.constraint_name, " +
            "  tc.table_name AS original_table, " +
            "  tc.column_name AS original_column, " +
            "  ccu.table_name AS referenced_table, " +
            "  ccu.column_name AS referenced_column " +
            "FROM information_schema.key_column_usage AS tc " +
            "  INNER JOIN information_schema.constraint_column_usage AS ccu " +
            "     ON tc.constraint_name = ccu.constraint_name " +
            "WHERE tc.constraint_schema = ? " +
            "AND tc.TABLE_NAME <> ccu.table_name " +
            "AND tc.table_name = ? ",
        "public", dataObject.getTableName()
    );

    while (resultSet.next()) {
      String constraintName = resultSet.getString("constraint_name");
      String originalColumn = resultSet.getString("original_column");
      String referencedTable = resultSet.getString("referenced_table");
      String referencedColumn = resultSet.getString("referenced_column");

      if (!"id".equals(referencedColumn.toLowerCase())) {
        addMismatch("Constraint pointing at a column that isn't 'id': '" + constraintName + "' pointing to table '" +
            referencedTable + "', column '" + referencedColumn + "'");
      }

      List<FieldValueForeignKey> eligibleForeignKeys = unfoundForeignKeys
          .stream()
          .filter(fk -> fk.getTableName().equals(referencedTable))
          .collect(Collectors.toList());

      if (eligibleForeignKeys.size() == 1) {
        FieldValueForeignKey foreignKey = eligibleForeignKeys.get(0);
        if (!originalColumn.equalsIgnoreCase(foreignKey.getFieldName())) {
          addMismatch(foreignKey, "DB constraint found to table '" + referencedTable + "', but column names don't match: '" +
              originalColumn + "' in DB, '" + foreignKey.getFieldName() + "' in Schema.");
        }
        unfoundForeignKeys.remove(foreignKey);
      } else {
        addMismatch("DB constraint '" + constraintName + "' exists, but " + eligibleForeignKeys.size() +
            " foreign keys exist in Schema pointing at table '" + referencedTable + "'. Expected exactly 1.");
      }
    }

    if (!unfoundForeignKeys.isEmpty()) {
      for (FieldValueForeignKey foreignKey : unfoundForeignKeys) {
        addMismatch(foreignKey, "ForeignKey restraint not found in DB.");
      }
    }
  }



  @SuppressWarnings("SimplifiableIfStatement")
  private Boolean matchesIgnoreCase(@Nullable String s1, @Nullable String s2) {
    if (s1 == null && s2 == null) {
      return true;
    } else if (s1 != null && s2 != null) {
      return s1.toLowerCase().equals(s2.toLowerCase());
    } else {
      return false;
    }
  }

  private void addMismatch(String message) {
    mismatches.add(new DataObjectMismatch(dataObject, null, message));
  }

  private void addMismatch(@NotNull FieldValue fieldValue, String message) {
    mismatches.add(new DataObjectMismatch(dataObject, fieldValue, message));
  }

}
