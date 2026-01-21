package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.DatabaseTest;
import com.mayhew3.postgresobject.db.DatabaseEnvironment;
import com.mayhew3.postgresobject.db.InternalDatabaseEnvironments;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.fest.assertions.api.Assertions.assertThat;

public class DataObjectCommitTest extends DatabaseTest {

  @Override
  public DatabaseEnvironment getTestEnvironment() {
    return InternalDatabaseEnvironments.test;
  }

  @Test
  public void testInsertDataObjectMock() throws SQLException {
    DataObjectMock dataObjectMock = new DataObjectMock();
    dataObjectMock.initializeForInsert();

    String titleValue = "Hugo";
    int kernelsValue = 15;

    dataObjectMock.title.changeValue(titleValue);
    dataObjectMock.kernels.changeValue(kernelsValue);

    assertThat(dataObjectMock.id.getValue())
        .as("SANITY: Expect id to be null before commit.")
        .isNull();

    dataObjectMock.commit(connection);

    Integer row_id = dataObjectMock.id.getValue();
    assertThat(row_id)
        .as("Expect id to be populated after commit.")
        .isNotNull();

    ResultSet resultSet = getDataObjectMockRow(row_id);
    assertThat(resultSet.next())
        .as("Expected to find row in DB after committing DataObject.")
        .isTrue();

    assertThat(resultSet.getString("title"))
        .as("Expected DB to have the value in DataObject.")
        .isEqualTo(titleValue);
    assertThat(resultSet.getInt("kernels"))
        .as("Expected DB to have the value in DataObject.")
        .isEqualTo(kernelsValue);
  }

  @Test
  public void testUpdateDataObjectMock() throws SQLException {
    DataObjectMock dataObjectMock = new DataObjectMock();
    dataObjectMock.initializeForInsert();

    String originalTitle = "Hugo";
    int originalKernels = 15;

    dataObjectMock.title.changeValue(originalTitle);
    dataObjectMock.kernels.changeValue(originalKernels);

    dataObjectMock.commit(connection);

    Integer row_id = dataObjectMock.id.getValue();

    ResultSet resultSet = getDataObjectMockRow(row_id);
    resultSet.next();

    DataObjectMock updateObject = new DataObjectMock();
    updateObject.initializeFromDBObject(resultSet);

    assertThat(updateObject.title.getValue())
        .as("Expect initializeFromDBObject to pull correct value from DB.")
        .isEqualTo(originalTitle);
    assertThat(updateObject.kernels.getValue())
        .as("Expect initializeFromDBObject to pull correct value from DB.")
        .isEqualTo(originalKernels);

    String updateTitle = "Barbers";
    Integer updateKernels = 782;

    updateObject.title.changeValue(updateTitle);
    updateObject.kernels.changeValue(updateKernels);

    updateObject.commit(connection);

    ResultSet updateResultSet = getDataObjectMockRow(row_id);
    updateResultSet.next();

    assertThat(updateResultSet.getString("title"))
        .as("Expected DB to have the value in DataObject.")
        .isEqualTo(updateTitle);
    assertThat(updateResultSet.getInt("kernels"))
        .as("Expected DB to have the value in DataObject.")
        .isEqualTo(updateKernels);
  }

  // private methods

  private ResultSet getDataObjectMockRow(Integer id) throws SQLException {
    return connection.prepareAndExecuteStatementFetch(
        "SELECT * FROM test WHERE id = ?", id
    );
  }
}
