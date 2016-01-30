package com.mayhew3.gamesutil.mediaobject;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.games.PostgresConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class MediaObjectPostgreSQLTest {

  private MediaObjectMock mediaObject;

  @Captor
  ArgumentCaptor<List<Object>> listCaptor;

  private static final Integer initial_id = 2;
  private static final String initial_title = "Taco Night!";
  private static final Integer initial_kernels = 42;

  @Before
  public void setUp() {
    mediaObject = new MediaObjectMock();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testInitializeForInsert() {
    assertThat(mediaObject.isInitialized())
        .as("SANITY: Expect initialized to be false before initialize method is called.")
        .isFalse();
    assertThat(mediaObject.isForInsert())
        .isFalse();
    assertThat(mediaObject.isForUpdate())
        .isFalse();

    mediaObject.initializeForInsert();

    assertThat(mediaObject.isInitialized())
        .as("Expect object to be initialized.")
        .isTrue();
    assertThat(mediaObject.isForInsert())
        .as("Initialized for insert.")
        .isTrue();
    assertThat(mediaObject.isForUpdate())
        .isFalse();
  }

  @Test
  public void testChangeToUpdateObject() {
    mediaObject.initializeForInsert();

    assertThat(mediaObject.isInitialized())
        .as("SANITY: Expect object to be initialized.")
        .isTrue();
    assertThat(mediaObject.isForInsert())
        .as("SANITY: Initialized for insert.")
        .isTrue();
    assertThat(mediaObject.isForUpdate())
        .isFalse();

    mediaObject.changeToUpdateObject();

    assertThat(mediaObject.isInitialized())
        .as("Expect object to still be initialized after change to 'update'.")
        .isTrue();
    assertThat(mediaObject.isForInsert())
        .as("Should no longer be for insert after change.")
        .isFalse();
    assertThat(mediaObject.isForUpdate())
        .as("Should be an update object after change.")
        .isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void testChangeToUpdateObjectThrowsExceptionIfNotInitialized() {
    mediaObject.changeToUpdateObject();
  }

  @Test
  public void testInitializeFromDBObject() throws SQLException {

    ResultSet resultSet = mockDBRow();

    mediaObject.initializeFromDBObject(resultSet);

    assertThat(mediaObject.isInitialized())
        .isTrue();
    assertThat(mediaObject.isForUpdate())
        .isTrue();
    assertThat(mediaObject.isForInsert())
        .isFalse();
    assertThat(mediaObject.id.getValue())
        .isEqualTo(initial_id);
    assertThat(mediaObject.title.getValue())
        .isEqualTo(initial_title);
    assertThat(mediaObject.kernels.getValue())
        .isEqualTo(initial_kernels);
  }


  @Test
  public void testSimpleInsert() throws SQLException {
    PostgresConnection connection = mock(PostgresConnection.class);
    PreparedStatement statement = prepareMockStatement(connection);

    mediaObject.initializeForInsert();

    String newTitle = "Booty";
    Integer newKernels = 46;
    mediaObject.title.changeValue(newTitle);
    mediaObject.kernels.changeValue(newKernels);

    mediaObject.commit(connection);

    verify(connection).getPreparedStatementWithReturnValue("INSERT INTO test (\"title\", \"kernels\") VALUES (?, ?)");
    verify(connection).executePreparedUpdateWithParamsWithoutClose(eq(statement), listCaptor.capture());

    assertThat(listCaptor.getValue())
        .contains(newTitle)
        .contains(newKernels)
        .hasSize(2);

    verify(statement).close();

    assertThat(mediaObject.isForUpdate())
        .isTrue();
    assertThat(mediaObject.isForInsert())
        .isFalse();
  }

  @Test
  public void testSimpleUpdate() throws SQLException {
    PostgresConnection connection = mock(PostgresConnection.class);

    ResultSet resultSet = mockDBRow();

    mediaObject.initializeFromDBObject(resultSet);

    String newTitle = "Booty plz";
    Integer newKernels = 113;
    mediaObject.title.changeValue(newTitle);
    mediaObject.kernels.changeValue(newKernels);

    mediaObject.commit(connection);

    verify(connection).prepareAndExecuteStatementUpdate(eq("UPDATE test SET \"title\" = ?, \"kernels\" = ? WHERE ID = ?"), listCaptor.capture());

    List<Object> actual = listCaptor.getValue();
    List<Object> expected = Lists.newArrayList((Object) newTitle, newKernels, initial_id);

    assertThat(actual)
        .isEqualTo(expected);

    assertThat(mediaObject.isForUpdate())
        .isTrue();
    assertThat(mediaObject.isForInsert())
        .isFalse();
  }

  @Test
  public void testUpdateWithNoChangedFieldsDoesNoDBOperations() throws SQLException {
    PostgresConnection connection = mock(PostgresConnection.class);

    ResultSet resultSet = mockDBRow();

    mediaObject.initializeFromDBObject(resultSet);
    mediaObject.commit(connection);

    verifyNoMoreInteractions(connection);

    assertThat(mediaObject.isForUpdate())
        .isTrue();
    assertThat(mediaObject.isForInsert())
        .isFalse();
  }


  
  // utility methods

  private PreparedStatement prepareMockStatement(PostgresConnection connection) throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(connection.getPreparedStatementWithReturnValue(anyString())).thenReturn(statement);
    when(statement.getGeneratedKeys()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getInt("id")).thenReturn(initial_id);
    return statement;
  }

  private ResultSet mockDBRow() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);

    when(resultSet.wasNull()).thenReturn(false);
    when(resultSet.getInt("id")).thenReturn(initial_id);
    when(resultSet.getObject("title")).thenReturn(initial_title);
    when(resultSet.getObject("kernels")).thenReturn(initial_kernels);
    return resultSet;
  }

  protected class MediaObjectMock extends MediaObjectPostgreSQL {
    public FieldValueString title = registerStringField("title");
    public FieldValueInteger kernels = registerIntegerField("kernels");

    @Override
    protected String getTableName() {
      return "test";
    }
  }
}