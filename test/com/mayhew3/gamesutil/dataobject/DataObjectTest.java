package com.mayhew3.gamesutil.dataobject;

import com.mayhew3.gamesutil.db.PostgresConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class DataObjectTest {

  private DataObjectMock mediaObject;

  @Captor
  ArgumentCaptor<List<Object>> listCaptor;

  @Captor
  ArgumentCaptor<List<FieldValue>> fieldValueCaptor;

  private static final Integer initial_id = 2;
  private static final String initial_title = "Taco Night!";
  private static final Integer initial_kernels = 42;

  @Before
  public void setUp() {
    mediaObject = new DataObjectMock();
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

    when(connection.prepareAndExecuteStatementInsertReturnId(anyString(), anyList())).thenReturn(initial_id);

    mediaObject.initializeForInsert();

    String newTitle = "Booty";
    Integer newKernels = 46;
    mediaObject.title.changeValue(newTitle);
    mediaObject.kernels.changeValue(newKernels);

    mediaObject.commit(connection);

    String sql = "INSERT INTO test (\"title\", \"kernels\") VALUES (?, ?)";
    verify(connection).prepareAndExecuteStatementInsertReturnId(eq(sql), fieldValueCaptor.capture());

    List<FieldValue> fieldValues = fieldValueCaptor.getValue();
    assertThat(fieldValues)
        .hasSize(2);

    FieldValue titleField = fieldValues.get(0);
    assertThat(titleField.getFieldName())
        .isEqualTo("title");
    assertThat(titleField.getChangedValue())
        .isEqualTo(newTitle);

    FieldValue kernelField = fieldValues.get(1);
    assertThat(kernelField.getFieldName())
        .isEqualTo("kernels");
    assertThat(kernelField.getChangedValue())
        .isEqualTo(newKernels);

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

    verify(connection).prepareAndExecuteStatementUpdateWithFields(eq("UPDATE test SET \"title\" = ?, \"kernels\" = ? WHERE ID = ?"), fieldValueCaptor.capture());

    List<FieldValue> fieldValues = fieldValueCaptor.getValue();
    assertThat(fieldValues)
        .hasSize(3);

    FieldValue titleField = fieldValues.get(0);
    assertThat(titleField.getFieldName())
        .isEqualTo("title");
    assertThat(titleField.getChangedValue())
        .isEqualTo(newTitle);

    FieldValue kernelField = fieldValues.get(1);
    assertThat(kernelField.getFieldName())
        .isEqualTo("kernels");
    assertThat(kernelField.getChangedValue())
        .isEqualTo(newKernels);

    FieldValue idField = fieldValues.get(2);
    assertThat(idField.getFieldName())
        .isEqualTo("id");
    assertThat(idField.getValue())
        .isEqualTo(initial_id);

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

  private ResultSet mockDBRow() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);

    when(resultSet.wasNull()).thenReturn(false);
    when(resultSet.getInt("id")).thenReturn(initial_id);
    when(resultSet.getString("title")).thenReturn(initial_title);
    when(resultSet.getInt("kernels")).thenReturn(initial_kernels);
    return resultSet;
  }

  protected class DataObjectMock extends DataObject {
    public FieldValueString title = registerStringField("title");
    public FieldValueInteger kernels = registerIntegerField("kernels");

    @Override
    protected String getTableName() {
      return "test";
    }
  }
}