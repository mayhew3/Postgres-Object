package com.mayhew3.gamesutil.dataobject;

import com.mayhew3.gamesutil.db.SQLConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseRecreator {
  private SQLConnection connection;

  public DatabaseRecreator(SQLConnection connection) {
    this.connection = connection;
  }
  
  public void recreateDatabase(DataSchema dataSchema) throws SQLException {
    List<String> tableNames = dataSchema.getAllTables().stream().map(DataObject::getTableName).collect(Collectors.toList());

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM information_schema.constraint_table_usage " +
            "WHERE table_schema = ?",
        "public"
    );
    while (resultSet.next()) {
      String table_name = resultSet.getString("table_name");
      if (tableNames.contains(table_name)) {
        String constraint_name = resultSet.getString("constraint_name");
        // todo: create statement needs to be able to add these back.
        connection.prepareAndExecuteStatementUpdate(
            "ALTER TABLE " + table_name + " DROP CONSTRAINT " + constraint_name
        );
      }
    }

    dropRecreateTables(dataSchema);
  }

  private void dropRecreateTables(DataSchema dataSchema) throws SQLException {
    for (DataObject dataObject : dataSchema.getAllTables()) {
      connection.prepareAndExecuteStatementUpdate(
          "DROP TABLE " + dataObject.getTableName()
      );

      String createStatement = dataObject.generateTableCreateStatement();

      connection.prepareAndExecuteStatementUpdate(createStatement);
    }
  }
}
