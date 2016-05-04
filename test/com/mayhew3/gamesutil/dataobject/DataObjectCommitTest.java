package com.mayhew3.gamesutil.dataobject;

import com.mayhew3.gamesutil.TVDatabaseTest;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.fest.assertions.Assertions.assertThat;

public class DataObjectCommitTest extends TVDatabaseTest {

  @Test
  public void testCommitDataObjectMock() throws SQLException {
    DataObjectMock dataObjectMock = new DataObjectMock();
    dataObjectMock.initializeForInsert();

    dataObjectMock.title.changeValue("Hugo");
    dataObjectMock.kernels.changeValue(15);

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


  }

  private ResultSet getDataObjectMockRow(Integer id) throws SQLException {
    return connection.prepareAndExecuteStatementFetch(
        "SELECT * FROM test WHERE id = ?", id
    );
  }
}
