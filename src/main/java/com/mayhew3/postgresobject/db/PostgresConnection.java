package com.mayhew3.postgresobject.db;

import com.google.common.collect.Lists;
import com.mayhew3.postgresobject.dataobject.FieldValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.postgresql.util.PSQLException;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

@SuppressWarnings({"rawtypes", "unused"})
public class PostgresConnection implements SQLConnection {

  private Connection _connection;
  private String _connectionString;
  private DateTime lastQueryExecuted;

  private static Logger logger = LogManager.getLogger(PostgresConnection.class);

  PostgresConnection(Connection connection, String connectionString) {
    _connection = connection;
    _connectionString = connectionString;
  }


  // Simple executes without use of PreparedStatement

  @Override
  public DatabaseType getDatabaseType() {
    return DatabaseType.POSTGRES;
  }

  @Override
  public String getSchemaName() {
    return "public";
  }

  @NotNull
  public ResultSet executeQuery(String sql) throws SQLException {
    checkConnection();

    ResultSet resultSet = executeSelectInternal(sql);

    updateLastExecuted();

    return resultSet;
  }

  @NotNull
  private ResultSet executeSelectInternal(String sql) throws SQLException {
    Statement statement = _connection.createStatement();
    try {
      return statement.executeQuery(sql);
    } catch (PSQLException e) {
      debug("Exception while executing query. Trying to reconnect...");
      resetConnection();
      return statement.executeQuery(sql);
    }
  }

  @NotNull
  public Statement executeUpdate(String sql) throws SQLException {
    checkConnection();

    Statement statement = _connection.createStatement();

    try {
      statement.executeUpdate(sql);
    } catch (PSQLException e) {
      debug("Exception while executing query. Trying to reconnect...");
      resetConnection();
      statement.executeUpdate(sql);
    }

    updateLastExecuted();

    return statement;
  }

  private void checkConnection() throws SQLException {
    if (_connection.isClosed()) {
      debug("Connection lost. Trying to reconnect...");
      resetConnection();
    } else if (isExpired()) {
      debug("30 minute threshold reached. Renewing connection.");
      try {
        closeConnection();
        debug("Connection closed. Resetting.");
      } catch (SQLException ignored) {
        debug("Tried to close connection, but failed.");
      }
      resetConnection();
    }
  }

  private void resetConnection() {
    // first, close existing connection so we don't explode active clients
    try {
      closeConnection();
    } catch (SQLException ignored) {}

    try {
      _connection = DriverManager.getConnection(_connectionString);
      updateLastExecuted();
      debug("Re-connect success!");
    } catch (SQLException e) {
      debug("Re-connect failed.");
      throw new RuntimeException("Failed to reconnect: " + e.getLocalizedMessage());
    }
  }

  public void closeConnection() throws SQLException {
    _connection.close();
  }


  // Full lifecycle operations using PreparedStatement



  @NotNull
  public ResultSet prepareAndExecuteStatementFetch(String sql, Object... params) throws SQLException {
    return prepareAndExecuteStatementFetch(sql, Lists.newArrayList(params));
  }

  @NotNull
  public ResultSet prepareAndExecuteStatementFetch(String sql, List<Object> params) throws SQLException {
    checkConnection();

    PreparedStatement preparedStatement = prepareStatementWithParams(sql, params);
    ResultSet resultSet = executePreparedStatement(preparedStatement);
    updateLastExecuted();
    return resultSet;
  }


  public Integer prepareAndExecuteStatementUpdate(String sql, Object... params) throws SQLException {
    return prepareAndExecuteStatementUpdate(sql, Lists.newArrayList(params));
  }


  public Integer prepareAndExecuteStatementUpdate(String sql, List<Object> params) throws SQLException {
    checkConnection();

    PreparedStatement preparedStatement = prepareStatementWithParams(sql, params);

    int rowsAffected = executePreparedUpdate(preparedStatement);
    preparedStatement.close();
    updateLastExecuted();
    return rowsAffected;
  }

  @Override
  public @NotNull PreparedStatement prepareStatementNoParams(String sql) throws SQLException {
    return _connection.prepareStatement(sql);
  }

  // Operations with user handle on PreparedStatement

  @NotNull
  public PreparedStatement prepareStatementWithParams(String sql, List<Object> params) throws SQLException {
    checkConnection();

    PreparedStatement preparedStatement = _connection.prepareStatement(sql);
    return plugParamsIntoStatement(preparedStatement, params);
  }


  @NotNull
  public ResultSet executePreparedStatementWithParams(PreparedStatement preparedStatement, Object... params) throws SQLException {
    List<Object> paramList = Lists.newArrayList(params);
    return executePreparedStatementWithParams(preparedStatement, paramList);
  }

  @NotNull
  public ResultSet executePreparedStatementWithParams(PreparedStatement preparedStatement, List<Object> params) throws SQLException {
    checkConnection();

    PreparedStatement statementWithParams = plugParamsIntoStatement(preparedStatement, params);
    ResultSet resultSet = executePreparedStatement(statementWithParams);
    updateLastExecuted();
    return resultSet;
  }

  @Override
  public void executePreparedUpdateWithParams(PreparedStatement preparedStatement, List<Object> paramList) throws SQLException {
    checkConnection();

    PreparedStatement statementWithParams = plugParamsIntoStatement(preparedStatement, paramList);
    executePreparedUpdate(statementWithParams);
    updateLastExecuted();
  }

  @Override
  public void executePreparedUpdateWithParams(PreparedStatement preparedStatement, Object... paramList) throws SQLException {
    executePreparedUpdateWithParams(preparedStatement, Lists.newArrayList(paramList));
  }

  private ResultSet executePreparedStatement(PreparedStatement preparedStatement) throws SQLException {
    try {
      return preparedStatement.executeQuery();
    } catch (PSQLException e) {
      debug("Exception while executing query. Trying to reconnect...");
      resetConnection();
      return preparedStatement.executeQuery();
    }
  }


  private int executePreparedUpdate(PreparedStatement preparedUpdate) throws SQLException {
    try {
      return preparedUpdate.executeUpdate();
    } catch (PSQLException e) {
      debug("Exception while executing query: " + e.getLocalizedMessage());
      debug("Trying to reconnect...");
      resetConnection();
      return preparedUpdate.executeUpdate();
    }
  }


  // Using FieldValue


  @NotNull
  public PreparedStatement prepareStatementWithFields(String sql, List<FieldValue> fields) throws SQLException {
    checkConnection();

    PreparedStatement preparedStatement = _connection.prepareStatement(sql);
    return plugFieldsIntoStatement(preparedStatement, fields);
  }

  public void prepareAndExecuteStatementUpdateWithFields(String sql, List<FieldValue> fields) throws SQLException {
    checkConnection();

    PreparedStatement preparedStatement = prepareStatementWithFields(sql, fields);

    executePreparedUpdate(preparedStatement);
    preparedStatement.close();
  }

  @NotNull
  public Integer prepareAndExecuteStatementInsertReturnId(String sql, List<FieldValue> fieldValues) throws SQLException {
    checkConnection();

    PreparedStatement preparedStatement = prepareStatementForInsertId(sql);
    plugFieldsIntoStatement(preparedStatement, fieldValues);

    executePreparedUpdate(preparedStatement);

    ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

    if (!generatedKeys.next()) {
      throw new RuntimeException("No rows in ResultSet from Inserted object!");
    }

    int id = generatedKeys.getInt("ID");
    preparedStatement.close();
    return id;
  }


  public void executePreparedUpdateWithFields(PreparedStatement preparedStatement, List<FieldValue> fieldValues) throws SQLException {
    checkConnection();

    plugFieldsIntoStatement(preparedStatement, fieldValues);
    executePreparedUpdate(preparedStatement);
    updateLastExecuted();
  }

  @Override
  public ResultSet getFKInfoForTable(String tableName) throws SQLException {
    return prepareAndExecuteStatementFetch(
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
        getSchemaName(), tableName
    );
  }

  @Override
  public ResultSet getIndexesForTable(String tableName) throws SQLException {
    return prepareAndExecuteStatementFetch(
        "SELECT indexname " +
            "FROM pg_indexes " +
            "WHERE schemaname = ? " +
            "AND tablename = ? ", getSchemaName(), tableName
    );
  }


  // unused but useful

  public boolean columnExists(String tableName, String columnName) throws SQLException {
    checkConnection();

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
      } else if (param instanceof Timestamp) {
        preparedStatement.setTimestamp(i, (Timestamp) param);
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
    checkConnection();

    return _connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
  }

  private void updateLastExecuted() {
    lastQueryExecuted = new DateTime();
  }

  private boolean isExpired() {
    DateTime threshold = DateTime.now().minusMinutes(30);
    return threshold.isAfter(lastQueryExecuted);
  }

  void debug(Object message) {
    logger.debug(message);
  }
}
