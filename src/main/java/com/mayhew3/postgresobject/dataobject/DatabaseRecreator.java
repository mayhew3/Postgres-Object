package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.SQLConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseRecreator {
  private SQLConnection connection;

  public DatabaseRecreator(SQLConnection connection) {
    this.connection = connection;
  }

  /**
   * Get the table name for use in SQL statements.
   * Does NOT include schema qualification - relies on search_path.
   */
  private String getQualifiedTableName(String tableName) {
    return tableName;
  }

  public void recreateDatabase(DataSchema dataSchema) throws SQLException {
    List<String> tableNames = dataSchema.getAllTables().stream().map(DataObject::getTableName).collect(Collectors.toList());

    dropAllForeignKeys(tableNames);
    dropAllTables(dataSchema);
    dropAllSequences(dataSchema);

    createAllTables(dataSchema);
    createAllForeignKeys(dataSchema);
    createAllIndices(dataSchema);
  }


  /* DROP METHODS */

  private void dropAllForeignKeys(List<String> tableNames) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM information_schema.table_constraints " +
            "WHERE table_schema = ? " +
            "AND constraint_type = ?",
        connection.getSchemaName(), "FOREIGN KEY"
    );
    while (resultSet.next()) {
      String table_name = resultSet.getString("table_name");
      if (tableNames.contains(table_name)) {
        String constraint_name = resultSet.getString("constraint_name");
        connection.prepareAndExecuteStatementUpdate(
            "ALTER TABLE " + getQualifiedTableName(table_name) + " DROP CONSTRAINT IF EXISTS " + constraint_name
        );
      }
    }
  }

  private void dropAllTables(DataSchema dataSchema) throws SQLException {
    for (DataObject dataObject : dataSchema.getAllTables()) {
      connection.prepareAndExecuteStatementUpdate(
          "DROP TABLE IF EXISTS " + getQualifiedTableName(dataObject.getTableName())
      );
    }
  }

  private void dropAllSequences(DataSchema dataSchema) throws SQLException {
    for (DataObject dataObject : dataSchema.getAllTables()) {
      for (String sequenceName : dataObject.getSequenceNames()) {
        connection.prepareAndExecuteStatementUpdate("DROP SEQUENCE IF EXISTS " + getQualifiedTableName(sequenceName));
      }
    }
  }


  /* CREATE METHODS */

  private void createAllTables(DataSchema dataSchema) throws SQLException {
    for (DataObject dataObject : dataSchema.getAllTables()) {
      String createStatement = dataObject.generateTableCreateStatement(connection.getDatabaseType());
      // No schema qualification - rely on search_path
      connection.prepareAndExecuteStatementUpdate(createStatement);
    }
  }

  private void createAllForeignKeys(DataSchema dataSchema) throws SQLException {
    for (DataObject dataObject : dataSchema.getAllTables()) {
      List<String> fkStatements = dataObject.generateAddForeignKeyStatements();
      for (String fkStatement : fkStatements) {
        // No schema qualification - rely on search_path
        connection.prepareAndExecuteStatementUpdate(fkStatement);
      }
    }
  }

  private void createAllIndices(DataSchema dataSchema) throws SQLException {
    for (DataObject dataObject : dataSchema.getAllTables()) {
      List<String> ixStatements = dataObject.generateAddIndexStatements();
      for (String ixStatement : ixStatements) {
        // No schema qualification - rely on search_path
        connection.prepareAndExecuteStatementUpdate(ixStatement);
      }
    }
  }
}
