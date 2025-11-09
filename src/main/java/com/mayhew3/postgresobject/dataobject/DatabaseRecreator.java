package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.SQLConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseRecreator {
  private static final Logger logger = LogManager.getLogger(DatabaseRecreator.class);
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
    String schemaName = connection.getSchemaName();
    logger.info("Recreating database with schema: {}", schemaName);

    // Verify search_path is set correctly
    ResultSet searchPathResult = connection.prepareAndExecuteStatementFetch("SHOW search_path");
    if (searchPathResult.next()) {
      String searchPath = searchPathResult.getString(1);
      logger.info("Current search_path: {}", searchPath);
    }

    List<String> tableNames = dataSchema.getAllTables().stream().map(DataObject::getTableName).collect(Collectors.toList());

    dropAllForeignKeys(tableNames);
    dropAllTables(dataSchema);
    dropAllSequences(dataSchema);

    createAllTables(dataSchema);
    createAllForeignKeys(dataSchema);
    createAllIndices(dataSchema);

    logger.info("Database recreation complete");
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
      logger.debug("Creating table: {}", createStatement.substring(0, Math.min(100, createStatement.length())));
      connection.prepareAndExecuteStatementUpdate(createStatement);

      // Verify which schema the table was created in
      String tableName = dataObject.getTableName();
      ResultSet schemaCheck = connection.prepareAndExecuteStatementFetch(
          "SELECT table_schema FROM information_schema.tables WHERE table_name = ?",
          tableName
      );
      if (schemaCheck.next()) {
        String actualSchema = schemaCheck.getString("table_schema");
        logger.info("Table {} created in schema: {}", tableName, actualSchema);
      } else {
        logger.warn("Table {} not found in information_schema after creation!", tableName);
      }
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
