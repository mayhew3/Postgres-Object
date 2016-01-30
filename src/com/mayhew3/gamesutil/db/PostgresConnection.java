package com.mayhew3.gamesutil.db;

import com.google.common.collect.Lists;
import com.sun.istack.internal.NotNull;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.List;

public class PostgresConnection {

  private Connection _connection;

  public PostgresConnection() {
    try {
      _connection = createConnection();
      System.out.println("Connection successful.");
    } catch (URISyntaxException | SQLException e) {
      e.printStackTrace();
      throw new RuntimeException("Connection refused.");
    }
  }

  public PostgresConnection(Connection connection) {
    _connection = connection;
  }


  private Connection createConnection() throws URISyntaxException, SQLException {
    String postgresURL = System.getenv("postgresURL");

    try {
      return DriverManager.getConnection(postgresURL);
    } catch (SQLException e) {
      URI dbUri = new URI(postgresURL);

      String username = dbUri.getUserInfo().split(":")[0];
      String password = dbUri.getUserInfo().split(":")[1];
      String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() +
          "?user=" + username + "&password=" + password + "&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";

      return DriverManager.getConnection(dbUrl);
    }
  }

  public Connection getConnection() {
    return _connection;
  }

  public void closeConnection() throws SQLException {
    _connection.close();
  }

  @NotNull
  public ResultSet executeQuery(String sql) throws SQLException {
    Statement statement = _connection.createStatement();
    return statement.executeQuery(sql);
  }

  @NotNull
  public Statement executeUpdate(String sql) throws SQLException {
    Statement statement = _connection.createStatement();

    statement.executeUpdate(sql);
    return statement;
  }

  public boolean columnExists(String tableName, String columnName) {
    try {
      ResultSet tables = _connection.getMetaData().getColumns(null, null, tableName, columnName);
      return tables.next();
    } catch (SQLException e) {
      throw new IllegalStateException("Error trying to find column " + columnName);
    }
  }

  public ResultSet prepareAndExecuteStatementFetch(String sql, Object... params) throws SQLException {
    return prepareAndExecuteStatementFetch(sql, Lists.newArrayList(params));
  }

  public ResultSet prepareAndExecuteStatementFetch(String sql, List<Object> params) throws SQLException {
    PreparedStatement preparedStatement = prepareStatement(sql, params);
    return preparedStatement.executeQuery();
  }

  public void prepareAndExecuteStatementUpdate(String sql, Object... params) throws SQLException {
    PreparedStatement preparedStatement = prepareStatement(sql, Lists.newArrayList(params));

    preparedStatement.executeUpdate();
    preparedStatement.close();
  }

  public void prepareAndExecuteStatementUpdate(String sql, List<Object> params) throws SQLException {
    PreparedStatement preparedStatement = prepareStatement(sql, params);

    preparedStatement.executeUpdate();
    preparedStatement.close();
  }

  public PreparedStatement prepareStatement(String sql, List<Object> params) throws SQLException {
    PreparedStatement preparedStatement = _connection.prepareStatement(sql);
    return plugParamsIntoStatement(preparedStatement, params);
  }

  public PreparedStatement getPreparedStatementWithReturnValue(String sql) throws SQLException {
    return _connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
  }

  public ResultSet executePreparedStatementWithParams(PreparedStatement preparedStatement, Object... params) throws SQLException {
    List<Object> paramList = Lists.newArrayList(params);
    return executePreparedStatementWithParams(preparedStatement, paramList);
  }

  public ResultSet executePreparedStatementWithParams(PreparedStatement preparedStatement, List<Object> params) throws SQLException {
    PreparedStatement statementWithParams = plugParamsIntoStatement(preparedStatement, params);
    return statementWithParams.executeQuery();
  }

  public void executePreparedUpdateWithParams(PreparedStatement preparedStatement, List<Object> paramList) throws SQLException {
    PreparedStatement statementWithParams = plugParamsIntoStatement(preparedStatement, paramList);
    statementWithParams.executeUpdate();
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

}
