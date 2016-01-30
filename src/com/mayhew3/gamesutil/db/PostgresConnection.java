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
  public ResultSet executeQuery(String sql) {
    try {
      Statement statement = _connection.createStatement();
      return statement.executeQuery(sql);
    } catch (SQLException e) {
      System.out.println("Error running SQL select: " + sql);
      e.printStackTrace();
      System.exit(-1);
    }
    return null;
  }

  @NotNull
  public Statement executeUpdate(String sql) {
    try {
      Statement statement = _connection.createStatement();

      statement.executeUpdate(sql);
      return statement;
    } catch (SQLException e) {
      throw new IllegalStateException("Error running SQL select: " + sql);
    }
  }

  public boolean hasMoreElements(ResultSet resultSet) {
    try {
      return resultSet.next();
    } catch (SQLException e) {
      throw new IllegalStateException("Error fetching next row from result set.");
    }
  }

  public int getInt(ResultSet resultSet, String columnName) {
    try {
      return resultSet.getInt(columnName);
    } catch (SQLException e) {
      throw new RuntimeException("Error trying to get integer column " + columnName + ": " + e.getLocalizedMessage());
    }
  }

  public String getString(ResultSet resultSet, String columnName) {
    try {
      return resultSet.getString(columnName);
    } catch (SQLException e) {
      throw new IllegalStateException("Error trying to get string column " + columnName);
    }
  }

  public boolean columnExists(String tableName, String columnName) {
    try {
      ResultSet tables = _connection.getMetaData().getColumns(null, null, tableName, columnName);
      return tables.next();
    } catch (SQLException e) {
      throw new IllegalStateException("Error trying to find column " + columnName);
    }
  }

  public ResultSet prepareAndExecuteStatementFetch(String sql, Object... params) {
    return prepareAndExecuteStatementFetch(sql, Lists.newArrayList(params));
  }

  public ResultSet prepareAndExecuteStatementFetch(String sql, List<Object> params) {
    PreparedStatement preparedStatement = prepareStatement(sql, params);
    try {
      return preparedStatement.executeQuery();
    } catch (SQLException e) {
      throw new RuntimeException("Error executing prepared statement for SQL: " + sql + ": " + e.getLocalizedMessage());
    }
  }

  public ResultSet prepareAndExecuteStatementFetchWithException(String sql, List<Object> params) throws SQLException {
    PreparedStatement preparedStatement = prepareStatement(sql, params);
    return preparedStatement.executeQuery();
  }

  public void prepareAndExecuteStatementUpdate(String sql, Object... params) {
    try {
      PreparedStatement preparedStatement = prepareStatement(sql, Lists.newArrayList(params));

      preparedStatement.executeUpdate();
      preparedStatement.close();
    } catch (SQLException e) {
      throw new RuntimeException("Error preparing statement for SQL: " + sql + ": " + e.getLocalizedMessage());
    }
  }

  public void prepareAndExecuteStatementUpdate(String sql, List<Object> params) {
    try {
      PreparedStatement preparedStatement = prepareStatement(sql, params);

      preparedStatement.executeUpdate();
      preparedStatement.close();
    } catch (SQLException e) {
      throw new RuntimeException("Error preparing and executing statement for SQL: " + sql + ": " + e.getLocalizedMessage());
    }
  }

  public PreparedStatement prepareStatement(String sql, List<Object> params) {
    PreparedStatement preparedStatement = getPreparedStatement(sql);
    try {
      return plugParamsIntoStatement(preparedStatement, params);
    } catch (SQLException e) {
      throw new RuntimeException("Error adding parameters to prepared statement for SQL: " + sql + ": " + e.getLocalizedMessage());
    }
  }

  public PreparedStatement getPreparedStatement(String sql) {
    try {
      return _connection.prepareStatement(sql);
    } catch (SQLException e) {
      throw new RuntimeException("Error preparing statement for SQL: " + sql + ": " + e.getLocalizedMessage());
    }
  }

  public PreparedStatement getPreparedStatementWithReturnValue(String sql) {
    try {
      return _connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    } catch (SQLException e) {
      throw new RuntimeException("Error preparing statement for SQL: " + sql + ": " + e.getLocalizedMessage());
    }
  }

  public ResultSet executePreparedStatementAlreadyHavingParameters(PreparedStatement preparedStatement) {
    try {
      return preparedStatement.executeQuery();
    } catch (SQLException e) {
      throw new RuntimeException("Error executing prepared statement. " + e.getLocalizedMessage());
    }
  }

  public ResultSet executePreparedStatementWithParams(PreparedStatement preparedStatement, Object... params) {
    List<Object> paramList = Lists.newArrayList(params);
    return executePreparedStatementWithParams(preparedStatement, paramList);
  }

  public ResultSet executePreparedStatementWithParams(PreparedStatement preparedStatement, List<Object> params) {
    try {
      PreparedStatement statementWithParams = plugParamsIntoStatement(preparedStatement, params);
      return statementWithParams.executeQuery();
    } catch (SQLException e) {
      throw new RuntimeException("Error executing prepared statement with params: " + params + ": " + e.getLocalizedMessage());
    }
  }

  public void executePreparedUpdateWithParamsWithoutClose(PreparedStatement preparedStatement, List<Object> paramList) {
    try {
      PreparedStatement statementWithParams = plugParamsIntoStatement(preparedStatement, paramList);
      statementWithParams.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Error executing prepared statement with params: " + paramList + ": " + e.getLocalizedMessage());
    }
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

  protected void setString(PreparedStatement preparedStatement, int index, String value) {
    try {
      preparedStatement.setString(index, value);
    } catch (SQLException e) {
      throw new RuntimeException("Error binding parameter " + index + " on statement to value " + value + ": " + e.getLocalizedMessage());
    }
  }

  public boolean hasConnection() {
    boolean isOpen;
    try {
      isOpen = _connection != null && !_connection.isClosed();
    } catch (SQLException e) {
      return false;
    }
    return isOpen;
  }
}
