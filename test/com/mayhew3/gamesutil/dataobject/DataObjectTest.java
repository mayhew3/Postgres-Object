package com.mayhew3.gamesutil.dataobject;

import com.mayhew3.gamesutil.db.PostgresConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class DataObjectTest {

  private DataObjectMock dataObject;

  @Captor
  ArgumentCaptor<List<Object>> listCaptor;

  @Captor
  ArgumentCaptor<List<FieldValue>> fieldValueCaptor;

  private static final Integer initial_id = 2;
  private static final String initial_title = "Taco Night!";
  private static final Integer initial_kernels = 42;

  @Before
  public void setUp() {
    dataObject = new DataObjectMock();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testInitializeForInsert() {
    assertThat(dataObject.isInitialized())
        .as("SANITY: Expect initialized to be false before initialize method is called.")
        .isFalse();
    assertThat(dataObject.isForInsert())
        .isFalse();
    assertThat(dataObject.isForUpdate())
        .isFalse();

    dataObject.initializeForInsert();

    assertThat(dataObject.isInitialized())
        .as("Expect object to be initialized.")
        .isTrue();
    assertThat(dataObject.isForInsert())
        .as("Initialized for insert.")
        .isTrue();
    assertThat(dataObject.isForUpdate())
        .isFalse();
  }

  @Test
  public void testChangeToUpdateObject() {
    dataObject.initializeForInsert();

    assertThat(dataObject.isInitialized())
        .as("SANITY: Expect object to be initialized.")
        .isTrue();
    assertThat(dataObject.isForInsert())
        .as("SANITY: Initialized for insert.")
        .isTrue();
    assertThat(dataObject.isForUpdate())
        .isFalse();

    dataObject.changeToUpdateObject();

    assertThat(dataObject.isInitialized())
        .as("Expect object to still be initialized after change to 'update'.")
        .isTrue();
    assertThat(dataObject.isForInsert())
        .as("Should no longer be for insert after change.")
        .isFalse();
    assertThat(dataObject.isForUpdate())
        .as("Should be an update object after change.")
        .isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void testChangeToUpdateObjectThrowsExceptionIfNotInitialized() {
    dataObject.changeToUpdateObject();
  }

  @Test
  public void testInitializeFromDBObject() throws SQLException {

    ResultSet resultSet = mockDBRow();

    dataObject.initializeFromDBObject(resultSet);

    assertThat(dataObject.isInitialized())
        .isTrue();
    assertThat(dataObject.isForUpdate())
        .isTrue();
    assertThat(dataObject.isForInsert())
        .isFalse();
    assertThat(dataObject.id.getValue())
        .isEqualTo(initial_id);
    assertThat(dataObject.title.getValue())
        .isEqualTo(initial_title);
    assertThat(dataObject.kernels.getValue())
        .isEqualTo(initial_kernels);
  }


  @Test
  public void testSimpleInsert() throws SQLException {
    PostgresConnection connection = mock(PostgresConnection.class);

    when(connection.prepareAndExecuteStatementInsertReturnId(anyString(), anyList())).thenReturn(initial_id);

    dataObject.initializeForInsert();

    String newTitle = "Booty";
    Integer newKernels = 46;
    dataObject.title.changeValue(newTitle);
    dataObject.kernels.changeValue(newKernels);

    dataObject.commit(connection);

    String sql = "INSERT INTO test (\"date_added\", \"title\", \"kernels\") VALUES (?, ?, ?)";
    verify(connection).prepareAndExecuteStatementInsertReturnId(eq(sql), fieldValueCaptor.capture());

    List<FieldValue> fieldValues = fieldValueCaptor.getValue();
    assertThat(fieldValues)
        .hasSize(3);

    FieldValue dateAddedField = fieldValues.get(0);
    assertThat(dateAddedField.getFieldName())
        .isEqualTo("date_added");
    assertThat(dateAddedField.getChangedValue())
        .isInstanceOf(Date.class)
        .isNotNull();

    FieldValue titleField = fieldValues.get(1);
    assertThat(titleField.getFieldName())
        .isEqualTo("title");
    assertThat(titleField.getChangedValue())
        .isEqualTo(newTitle);

    FieldValue kernelField = fieldValues.get(2);
    assertThat(kernelField.getFieldName())
        .isEqualTo("kernels");
    assertThat(kernelField.getChangedValue())
        .isEqualTo(newKernels);

    assertThat(dataObject.isForUpdate())
        .isTrue();
    assertThat(dataObject.isForInsert())
        .isFalse();
  }

  @Test
  public void testSimpleUpdate() throws SQLException {
    PostgresConnection connection = mock(PostgresConnection.class);

    ResultSet resultSet = mockDBRow();

    dataObject.initializeFromDBObject(resultSet);

    String newTitle = "Booty plz";
    Integer newKernels = 113;
    dataObject.title.changeValue(newTitle);
    dataObject.kernels.changeValue(newKernels);

    dataObject.commit(connection);

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

    assertThat(dataObject.isForUpdate())
        .isTrue();
    assertThat(dataObject.isForInsert())
        .isFalse();
  }

  @Test
  public void testUpdateWithNoChangedFieldsDoesNoDBOperations() throws SQLException {
    PostgresConnection connection = mock(PostgresConnection.class);

    ResultSet resultSet = mockDBRow();

    dataObject.initializeFromDBObject(resultSet);
    dataObject.commit(connection);

    verifyNoMoreInteractions(connection);

    assertThat(dataObject.isForUpdate())
        .isTrue();
    assertThat(dataObject.isForInsert())
        .isFalse();
  }

  @Test
  public void testGenerateTableCreateStatement() throws SQLException {
    String ddl = dataObject.generateTableCreateStatement();

    assertThat(ddl)
        .isEqualTo("CREATE TABLE test (id serial NOT NULL, date_added TIMESTAMP(6) WITHOUT TIME ZONE, title TEXT NOT NULL, kernels INTEGER, PRIMARY KEY (id))");
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
    public FieldValueString title = registerStringField("title", Nullability.NOT_NULL);
    public FieldValueInteger kernels = registerIntegerField("kernels", Nullability.NULLABLE);

    @Override
    protected String getTableName() {
      return "test";
    }
  }
}