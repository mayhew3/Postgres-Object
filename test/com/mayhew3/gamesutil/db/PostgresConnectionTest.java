package com.mayhew3.gamesutil.db;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.db.PostgresConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.sql.*;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class PostgresConnectionTest {

  private Connection connection;
  private PostgresConnection postgresConnection;

  @Before
  public void setUp() {
    connection = mock(Connection.class);
    postgresConnection = new PostgresConnection(connection);
  }

  @Test
  public void testCloseConnection() throws SQLException {
    postgresConnection.closeConnection();
    verify(connection).close();
  }

  @Test
  public void testExecuteQuery() throws SQLException {
    String sql = "UPDATE test";

    Statement statement = mock(Statement.class);
    ResultSet resultSet = mock(ResultSet.class);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(sql)).thenReturn(resultSet);

    ResultSet actual = postgresConnection.executeQuery(sql);

    verify(connection).createStatement();
    verify(statement).executeQuery(sql);

    assertThat(actual)
        .isEqualTo(resultSet);
  }


  @Test
  public void testExecuteUpdate() {

  }

  @Test
  public void testHasMoreElements() {

  }

  @Test
  public void testGetInt() {

  }

  @Test
  public void testGetString() {

  }

  @Test
  public void testColumnExists() {

  }

  @Test
  public void testPrepareAndExecuteStatementFetch() {

  }

  @Test
  public void testPrepareAndExecuteStatementFetch1() {

  }

  @Test
  public void testPrepareAndExecuteStatementFetchWithException() {

  }

  @Test
  public void testPrepareAndExecuteStatementUpdate() {

  }

  @Test
  public void testPrepareAndExecuteStatementUpdate1() {

  }

  @Test
  public void testPrepareStatement() {

  }

  @Test
  public void testGetPreparedStatement() {

  }

  @Test
  public void testGetPreparedStatementWithReturnValue() {

  }

  @Test
  public void testExecutePreparedStatementAlreadyHavingParameters() {

  }

  @Test
  public void testExecutePreparedStatementWithParams() {

  }

  @Test
  public void testExecutePreparedStatementWithParams1() {

  }

  @Test
  public void testExecutePreparedUpdateWithParamsWithoutClose() {

  }
}