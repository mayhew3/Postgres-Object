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
   * Get the fully qualified table name (schema.table) for use in SQL statements.
   * For databases without schema support or when using default schema, returns just the table name.
   * Uses quoted identifiers to ensure proper parsing by PostgreSQL.
   */
  private String getQualifiedTableName(String tableName) {
    String schemaName = connection.getSchemaName();
    if (schemaName != null && !schemaName.isEmpty() && !schemaName.equals("public")) {
      return "\"" + schemaName + "\".\"" + tableName + "\"";
    }
    return "\"" + tableName + "\"";
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

      // Schema-qualify CREATE TABLE statements for non-public schemas
      String schemaName = connection.getSchemaName();
      if (schemaName != null && !schemaName.isEmpty() && !schemaName.equals("public")) {
        String tableName = dataObject.getTableName();
        createStatement = createStatement.replace("CREATE TABLE " + tableName,
                                                   "CREATE TABLE " + getQualifiedTableName(tableName));
      }

      connection.prepareAndExecuteStatementUpdate(createStatement);
    }
  }

  private void createAllForeignKeys(DataSchema dataSchema) throws SQLException {
    for (DataObject dataObject : dataSchema.getAllTables()) {
      List<String> fkStatements = dataObject.generateAddForeignKeyStatements();
      for (String fkStatement : fkStatements) {
        // Schema-qualify table names in ALTER TABLE and REFERENCES clauses
        String qualifiedStatement = qualifyTableNamesInStatement(fkStatement, dataObject.getTableName());
        connection.prepareAndExecuteStatementUpdate(qualifiedStatement);
      }
    }
  }

  private void createAllIndices(DataSchema dataSchema) throws SQLException {
    for (DataObject dataObject : dataSchema.getAllTables()) {
      List<String> ixStatements = dataObject.generateAddIndexStatements();
      for (String ixStatement : ixStatements) {
        // Schema-qualify table name in ON clause
        String qualifiedStatement = qualifyTableNamesInStatement(ixStatement, dataObject.getTableName());
        connection.prepareAndExecuteStatementUpdate(qualifiedStatement);
      }
    }
  }

  /**
   * Replace unqualified table names in SQL statements with schema-qualified names.
   * Handles ALTER TABLE, REFERENCES, and ON clauses.
   */
  private String qualifyTableNamesInStatement(String statement, String tableName) {
    String schemaName = connection.getSchemaName();
    if (schemaName == null || schemaName.isEmpty() || schemaName.equals("public")) {
      return statement;
    }

    String qualifiedName = getQualifiedTableName(tableName);
    // Replace in ALTER TABLE, REFERENCES, and ON clauses
    statement = statement.replace("ALTER TABLE " + tableName, "ALTER TABLE " + qualifiedName);
    statement = statement.replace("REFERENCES " + tableName, "REFERENCES " + qualifiedName);
    statement = statement.replace("ON " + tableName, "ON " + qualifiedName);
    return statement;
  }
}
