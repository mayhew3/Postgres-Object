package com.mayhew3.postgresobject.db;

import com.google.common.collect.Lists;
import com.mayhew3.postgresobject.dataobject.FieldValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PostgresConnectionTest {

  private Connection connection;
  private PostgresConnection postgresConnection;

  @BeforeEach
  public void setUp() {
    connection = mock(Connection.class);
    postgresConnection = new PostgresConnection(connection, "test", null);
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
  public void testColumnExists() {

  }

  @Test
  public void testPrepareAndExecuteStatementFetch() {

  }

  @Test
  public void testPrepareAndExecuteStatementFetch1() {

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
  public void testGetPreparedStatementWithReturnValue() {

  }

  @SuppressWarnings("ConstantConditions")
  @Test
  public void testExecutePreparedStatementWithParams() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    Timestamp timestamp = new Timestamp(new java.util.Date().getTime());
    Integer integer = 4;
    String string = "Test!";
    BigDecimal bigDecimal = BigDecimal.valueOf(12.34);
    Boolean ohBoolean = true;

    ResultSet resultSet = mock(ResultSet.class);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    List<Object> params = Lists.newArrayList((Object) timestamp, integer, string, bigDecimal, ohBoolean);

    postgresConnection.executePreparedStatementWithParams(preparedStatement, params);

    verify(preparedStatement).setTimestamp(1, timestamp);
    verify(preparedStatement).setInt(2, integer);
    verify(preparedStatement).setString(3, string);
    verify(preparedStatement).setBigDecimal(4, bigDecimal);
    verify(preparedStatement).setBoolean(5, ohBoolean);

    verify(preparedStatement).executeQuery();
    verify(preparedStatement, never()).close();
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  public void testExecutePreparedStatementWithParams1() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    Timestamp timestamp = new Timestamp(new java.util.Date().getTime());
    Integer integer = 4;
    String string = "Test!";
    BigDecimal bigDecimal = BigDecimal.valueOf(12.34);
    Boolean ohBoolean = true;

    ResultSet resultSet = mock(ResultSet.class);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    postgresConnection.executePreparedStatementWithParams(preparedStatement, timestamp, integer, string, bigDecimal, ohBoolean);

    verify(preparedStatement).setTimestamp(1, timestamp);
    verify(preparedStatement).setInt(2, integer);
    verify(preparedStatement).setString(3, string);
    verify(preparedStatement).setBigDecimal(4, bigDecimal);
    verify(preparedStatement).setBoolean(5, ohBoolean);

    verify(preparedStatement).executeQuery();
    verify(preparedStatement, never()).close();
  }

  @Test
  public void testExecutePreparedUpdateWithFields() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);

    FieldValue firstField = mock(FieldValue.class);
    FieldValue secondField = mock(FieldValue.class);

    List<FieldValue> fields = Lists.newArrayList(firstField, secondField);

    postgresConnection.executePreparedUpdateWithFields(preparedStatement, fields);

    verify(firstField).updatePreparedStatement(preparedStatement, 1);
    verify(secondField).updatePreparedStatement(preparedStatement, 2);

    verify(preparedStatement).executeUpdate();
    verify(preparedStatement, never()).close();
  }

  @Test
  public void testPrepareAndExecuteStatementUpdateWithFields() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);

    String sql = "INSERT NOTHING";
    when(connection.prepareStatement(sql)).thenReturn(preparedStatement);

    FieldValue firstField = mock(FieldValue.class);
    FieldValue secondField = mock(FieldValue.class);

    List<FieldValue> fields = Lists.newArrayList(firstField, secondField);

    postgresConnection.prepareAndExecuteStatementUpdateWithFields(sql, fields);

    verify(firstField).updatePreparedStatement(preparedStatement, 1);
    verify(secondField).updatePreparedStatement(preparedStatement, 2);

    verify(preparedStatement).executeUpdate();
    verify(preparedStatement).close();
  }

  @Test
  public void testExecutePreparedUpdateWithParams() {

  }
}