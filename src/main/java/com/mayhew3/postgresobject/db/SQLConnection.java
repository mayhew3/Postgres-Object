package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.dataobject.FieldValue;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@SuppressWarnings({"JavaDoc", "unused", "UnusedReturnValue", "rawtypes"})
public interface SQLConnection {

  DatabaseType getDatabaseType();

  String getSchemaName();

  // Simple executes without use of PreparedStatements.

  @NotNull
  ResultSet executeQuery(String sql) throws SQLException;

  @NotNull
  Statement executeUpdate(String sql) throws SQLException;

  void closeConnection() throws SQLException;


  // Full lifecycle operations using PreparedStatement

  /**
   * - Create PreparedStatement using given SQL.
   * - Plug given parameters into PreparedStatement.
   * - Execute PreparedStatement against DB.
   *
   * (Note: PreparedStatement is not closed because that would close the associated ResultSet. If this is
   * an issue, try using the methods to get a handle on the PreparedStatement so close() can be called when
   * finished.)
   *
   * @param sql SQL query that should be run. Should be SELECT query.
   * @param params Vararg of parameters that should be plugged into query.
   * @return ResultSet with results of executed query.
   * @throws SQLException
   */
  @NotNull
  ResultSet prepareAndExecuteStatementFetch(String sql, Object... params) throws SQLException;

  /**
   * - Create PreparedStatement using given SQL.
   * - Plug given parameters into PreparedStatement.
   * - Execute PreparedStatement against DB.
   *
   * (Note: PreparedStatement is not closed because that would close the associated ResultSet. If this is
   * an issue, try using the methods to get a handle on the PreparedStatement so close() can be called when
   * finished.)
   *
   * @param sql SQL query that should be run. Should be SELECT query.
   * @param params List of parameters that should be plugged into query.
   * @return ResultSet with results of executed query.
   * @throws SQLException
   */
  @NotNull
  ResultSet prepareAndExecuteStatementFetch(String sql, List<Object> params) throws SQLException;

  /**
   * - Create PreparedStatement using given SQL.
   * - Plug given parameters into PreparedStatement.
   * - Execute PreparedStatement against DB.
   * - Close PreparedStatement.
   *
   * @param sql SQL query that should be run. Should be INSERT or UPDATE query.
   * @param params Vararg of parameters that should be plugged into query.
   * @return Number of rows affected by update.
   * @throws SQLException
   */
  Integer prepareAndExecuteStatementUpdate(String sql, Object... params) throws SQLException;

  /**
   * - Create PreparedStatement using given SQL.
   * - Plug given parameters into PreparedStatement.
   * - Execute PreparedStatement against DB.
   * - Close PreparedStatement.
   *
   * @param sql SQL query that should be run. Should be INSERT or UPDATE query.
   * @param params List of parameters that should be plugged into query.
   * @return Number of rows affected by update.
   * @throws SQLException
   */
  Integer prepareAndExecuteStatementUpdate(String sql, List<Object> params) throws SQLException;




  // Operations with user handle on PreparedStatement

  /**
   * - Create PreparedStatement using SQL.
   *
   * @param sql SQL query that should be run
   * @return PreparedStatement object
   * @throws SQLException
   */
  @NotNull
  PreparedStatement prepareStatementNoParams(String sql) throws SQLException;

  /**
   * - Create PreparedStatement using SQL.
   * - Plug given parameters into PreparedStatement.
   *
   * @param sql SQL query that should be run
   * @param params List of parameters that should be plugged into query.
   * @return PreparedStatement object
   * @throws SQLException
   */
  @NotNull
  PreparedStatement prepareStatementWithParams(String sql, List<Object> params) throws SQLException;

  /**
   * - Plug given parameters into given PreparedStatement.
   * - Execute PreparedStatement. (NOTE: doesn't close PreparedStatement. Be sure to close it when done.)
   *
   * @param preparedStatement that will be parameterized and executed.
   * @param params vararg of parameters that will be plugged in.
   * @return ResultSet of data from query
   * @throws SQLException
   */
  @NotNull
  ResultSet executePreparedStatementWithParams(PreparedStatement preparedStatement, Object... params) throws SQLException;

  /**
   * - Plug given parameters into given PreparedStatement.
   * - Execute PreparedStatement. (NOTE: doesn't close PreparedStatement. Be sure to close it when done.)
   *
   * @param preparedStatement that will be parameterized and executed.
   * @param params List of parameters that will be plugged in.
   * @return ResultSet of data from query
   * @throws SQLException
   */
  @NotNull
  ResultSet executePreparedStatementWithParams(PreparedStatement preparedStatement, List<Object> params) throws SQLException;

  /**
   * - Plug given parameters into given PreparedStatement.
   * - Execute PreparedStatement. (NOTE: doesn't close PreparedStatement. Be sure to close it when done.)
   *
   * @param preparedStatement that will be parameterized and executed.
   * @param paramList List of parameters that will be plugged in.
   * @throws SQLException
   */
  void executePreparedUpdateWithParams(PreparedStatement preparedStatement, List<Object> paramList) throws SQLException;

  /**
   * - Plug given parameters into given PreparedStatement.
   * - Execute PreparedStatement. (NOTE: doesn't close PreparedStatement. Be sure to close it when done.)
   *
   * @param preparedStatement that will be parameterized and executed.
   * @param paramList List of parameters that will be plugged in.
   * @throws SQLException
   */
  void executePreparedUpdateWithParams(PreparedStatement preparedStatement, Object... paramList) throws SQLException;


  // Using FieldValue

  /**
   * - Create PreparedStatement using SQL.
   * - Plug given parameters into PreparedStatement.
   *
   * @param sql SQL query that should be run
   * @param fields List of FieldValues that should be plugged into query.
   * @return PreparedStatement object
   * @throws SQLException
   */
  @NotNull
  PreparedStatement prepareStatementWithFields(String sql, List<FieldValue> fields) throws SQLException;

  /**
   * - Create PreparedStatement using given SQL.
   * - Plug given parameters into PreparedStatement.
   * - Execute PreparedStatement against DB.
   * - Close PreparedStatement.
   *
   * @param sql SQL query that should be run. Should be INSERT or UPDATE query.
   * @param fields List of FieldValues that should be plugged into query.
   * @throws SQLException
   */
  void prepareAndExecuteStatementUpdateWithFields(String sql, List<FieldValue> fields) throws SQLException;

  /**
   * - Create PreparedStatement using given SQL, with Generated Keys enabled.
   * - Plug given parameters into PreparedStatement.
   * - Execute PreparedStatement again DB.
   * - Close PreparedStatement.
   * - Return ID of newly inserted row.
   *
   * @param sql SQL query that should be run. Should be INSERT query.
   * @param fieldValues List of FieldValues that should be plugged into query.
   * @return ID of newly inserted row.
   * @throws SQLException
   */
  @NotNull
  Integer prepareAndExecuteStatementInsertReturnId(String sql, List<FieldValue> fieldValues) throws SQLException;

  /**
   * - Plug given parameters into given PreparedStatement.
   * - Execute PreparedStatement. (NOTE: doesn't close PreparedStatement. Be sure to close it when done.)
   *
   * @param preparedStatement that will be parameterized and executed. Should be INSERT or UPDATE query.
   * @param fieldValues List of FieldValues that will be plugged in.
   * @throws SQLException
   */
  void executePreparedUpdateWithFields(PreparedStatement preparedStatement, List<FieldValue> fieldValues) throws SQLException;


  ResultSet getFKInfoForTable(String tableName) throws SQLException;

  ResultSet getIndexesForTable(String tableName) throws SQLException;
}
