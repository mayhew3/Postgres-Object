package com.mayhew3.postgresobject.db;


import com.google.common.collect.Lists;
import com.mayhew3.postgresobject.dataobject.FieldValue;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings({"unused", "rawtypes"})
public class MySQLConnection implements SQLConnection {

  private Logger logger = Logger.getLogger(MySQLConnection.class.getName());

  private Connection _connection;
  private final String schemaName;

  MySQLConnection(Connection connection, String schemaName) {
    _connection = connection;
    this.schemaName = schemaName;
  }


  // Simple executes without use of PreparedStatement

  @Override
  public DatabaseType getDatabaseType() {
    return DatabaseType.MYSQL;
  }

  @Override
  public String getSchemaName() {
    return this.schemaName;
  }

  @Override
  @NotNull
  public ResultSet executeQuery(String sql) throws SQLException {
    Statement statement = _connection.createStatement();
    return statement.executeQuery(sql);
  }

  @Override
  @NotNull
  public Statement executeUpdate(String sql) throws SQLException {
    Statement statement = _connection.createStatement();

    statement.executeUpdate(sql);
    return statement;
  }

  @Override
  public void closeConnection() throws SQLException {
    _connection.close();
  }


  // Full lifecycle operations using PreparedStatement



  @NotNull
  @Override
  public ResultSet prepareAndExecuteStatementFetch(String sql, Object... params) throws SQLException {
    return prepareAndExecuteStatementFetch(sql, Lists.newArrayList(params));
  }

  @NotNull
  @Override
  public ResultSet prepareAndExecuteStatementFetch(String sql, List<Object> params) throws SQLException {
    PreparedStatement preparedStatement = prepareStatementWithParams(sql, params);
//    logger.log(Level.INFO, preparedStatement.toString());
    return preparedStatement.executeQuery();
  }


  @Override
  public Integer prepareAndExecuteStatementUpdate(String sql, Object... params) throws SQLException {
    return prepareAndExecuteStatementUpdate(sql, Lists.newArrayList(params));
  }

  @Override
  public Integer prepareAndExecuteStatementUpdate(String sql, List<Object> params) throws SQLException {
    PreparedStatement preparedStatement = prepareStatementWithParams(sql, params);
//    logger.log(Level.INFO, preparedStatement.toString());
    int updatedRowCount = preparedStatement.executeUpdate();
    preparedStatement.close();
    return updatedRowCount;
  }

  @Override
  public @NotNull PreparedStatement prepareStatementNoParams(String sql) throws SQLException {
    return _connection.prepareStatement(sql);
  }

  // Operations with user handle on PreparedStatement

  @NotNull
  @Override
  public PreparedStatement prepareStatementWithParams(String sql, List<Object> params) throws SQLException {
    PreparedStatement preparedStatement = _connection.prepareStatement(sql);
    return plugParamsIntoStatement(preparedStatement, params);
  }


  @NotNull
  @Override
  public ResultSet executePreparedStatementWithParams(PreparedStatement preparedStatement, Object... params) throws SQLException {
    List<Object> paramList = Lists.newArrayList(params);
    return executePreparedStatementWithParams(preparedStatement, paramList);
  }

  @NotNull
  @Override
  public ResultSet executePreparedStatementWithParams(PreparedStatement preparedStatement, List<Object> params) throws SQLException {
    PreparedStatement statementWithParams = plugParamsIntoStatement(preparedStatement, params);
    return statementWithParams.executeQuery();
  }

  @Override
  public void executePreparedUpdateWithParams(PreparedStatement preparedStatement, Object... paramList) throws SQLException {
    executePreparedUpdateWithParams(preparedStatement, Lists.newArrayList(paramList));
  }

  @Override
  public void executePreparedUpdateWithParams(PreparedStatement preparedStatement, List<Object> paramList) throws SQLException {
    PreparedStatement statementWithParams = plugParamsIntoStatement(preparedStatement, paramList);
    statementWithParams.executeUpdate();
  }



  // Using FieldValue


  @NotNull
  @Override
  public PreparedStatement prepareStatementWithFields(String sql, List<FieldValue> fields) throws SQLException {
    PreparedStatement preparedStatement = _connection.prepareStatement(sql);
    return plugFieldsIntoStatement(preparedStatement, fields);
  }

  @Override
  public void prepareAndExecuteStatementUpdateWithFields(String sql, List<FieldValue> fields) throws SQLException {
    PreparedStatement preparedStatement = prepareStatementWithFields(sql, fields);

    preparedStatement.executeUpdate();
    preparedStatement.close();
  }

  @NotNull
  @Override
  public Integer prepareAndExecuteStatementInsertReturnId(String sql, List<FieldValue> fieldValues) throws SQLException {
    PreparedStatement preparedStatement = prepareStatementForInsertId(sql);
    plugFieldsIntoStatement(preparedStatement, fieldValues);

    preparedStatement.executeUpdate();

    ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

    if (!generatedKeys.next()) {
      throw new RuntimeException("No rows in ResultSet from Inserted object!");
    }

    int id = generatedKeys.getInt(1);
    preparedStatement.close();
    return id;
  }


  @Override
  public void executePreparedUpdateWithFields(PreparedStatement preparedStatement, List<FieldValue> fieldValues) throws SQLException {
    plugFieldsIntoStatement(preparedStatement, fieldValues);
    preparedStatement.executeUpdate();
  }

  @Override
  public ResultSet getFKInfoForTable(String tableName) throws SQLException {
    return prepareAndExecuteStatementFetch(
        "SELECT " +
            "  tc.constraint_name, " +
            "  tc.table_name AS original_table, " +
            "  tc.column_name AS original_column, " +
            "  tc.REFERENCED_TABLE_NAME as referenced_table, " +
            "  tc.REFERENCED_COLUMN_NAME as referenced_column " +
            "FROM information_schema.key_column_usage AS tc " +
            "WHERE tc.constraint_schema = ? " +
            "AND tc.REFERENCED_TABLE_NAME IS NOT NULL " +
            "AND tc.table_name = ? ",
        getSchemaName(), tableName
    );
  }

  @Override
  public ResultSet getIndexesForTable(String tableName) throws SQLException {
    return prepareAndExecuteStatementFetch(
        "SELECT DISTINCT INDEX_NAME AS indexname " +
            "FROM information_schema.STATISTICS " +
            "WHERE TABLE_SCHEMA = ? " +
            "AND TABLE_NAME = ? ", getSchemaName(), tableName
    );
  }


  // unused but useful

  public boolean columnExists(String tableName, String columnName) throws SQLException {
    return _connection.getMetaData().getColumns(null, null, tableName, columnName).next();
  }



  // utility methods

  private PreparedStatement plugFieldsIntoStatement(PreparedStatement preparedStatement, List<FieldValue> fieldValues) throws SQLException {
    int i = 1;
    for (FieldValue fieldValue : fieldValues) {
      fieldValue.updatePreparedStatement(preparedStatement, i);
      i++;
    }
    return preparedStatement;
  }

  private PreparedStatement plugParamsIntoStatement(PreparedStatement preparedStatement, List<Object> params) throws SQLException {
    int i = 1;
    for (Object param : params) {
      if (param instanceof String) {
        preparedStatement.setString(i, (String) param);
      } else if (param instanceof Integer) {
        preparedStatement.setInt(i, (Integer) param);
      } else if (param instanceof BigDecimal) {
        preparedStatement.setBigDecimal(i, (BigDecimal) param);
      } else if (param instanceof Double) {
        preparedStatement.setDouble(i, (Double) param);
      } else if (param instanceof Date) {
        preparedStatement.setDate(i, (Date) param);
      } else if (param instanceof Boolean) {
        preparedStatement.setBoolean(i, (Boolean) param);
      } else {
        throw new RuntimeException("Unknown type of param: " + param.getClass());
      }
      i++;
    }
    return preparedStatement;
  }

  PreparedStatement prepareStatementForInsertId(String sql) throws SQLException {
    return _connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
  }

}
