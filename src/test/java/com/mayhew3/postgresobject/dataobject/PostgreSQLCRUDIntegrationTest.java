package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.DatabaseTest;
import com.mayhew3.postgresobject.db.DatabaseEnvironment;
import com.mayhew3.postgresobject.db.InternalDatabaseEnvironments;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for PostgreSQL CRUD operations covering Phase 2.3 requirements.
 * These tests require a PostgreSQL database connection and are excluded from CI.
 */
public class PostgreSQLCRUDIntegrationTest extends DatabaseTest {

  @Override
  public DatabaseEnvironment getTestEnvironment() {
    return InternalDatabaseEnvironments.test;
  }

  private ResultSet getRow(Integer id) throws SQLException {
    return connection.prepareAndExecuteStatementFetch(
        "SELECT * FROM test WHERE id = ?",
        id
    );
  }

  // CREATE Tests

  @Test
  public void testFullCRUDCycle() throws SQLException {
    // CREATE: Insert with all field types
    DataObjectMock dataObject = new DataObjectMock();
    dataObject.initializeForInsert();
    dataObject.title.changeValue("Test Title");
    dataObject.kernels.changeValue(42);
    dataObject.commit(connection);

    Integer id = dataObject.id.getValue();
    assertThat(id).isNotNull().isGreaterThan(0);

    // READ: Verify insertion
    ResultSet rs = getRow(id);
    assertThat(rs.next()).isTrue();
    assertThat(rs.getString("title")).isEqualTo("Test Title");
    assertThat(rs.getInt("kernels")).isEqualTo(42);

    // UPDATE: Modify fields
    DataObjectMock updateObject = new DataObjectMock();
    ResultSet rsForUpdate = getRow(id);
    rsForUpdate.next();
    updateObject.initializeFromDBObject(rsForUpdate);
    updateObject.title.changeValue("Updated Title");
    updateObject.kernels.changeValue(99);
    updateObject.commit(connection);

    // VERIFY UPDATE
    ResultSet rsUpdated = getRow(id);
    rsUpdated.next();
    assertThat(rsUpdated.getString("title")).isEqualTo("Updated Title");
    assertThat(rsUpdated.getInt("kernels")).isEqualTo(99);

    // DELETE
    connection.prepareAndExecuteStatementUpdate(
        "DELETE FROM test WHERE id = ?",
        id
    );

    // VERIFY DELETE
    assertThat(getRow(id).next()).isFalse();
  }

  @Test
  public void testNullHandling() throws SQLException {
    // Insert with default value for nullable field
    DataObjectMock dataObject = new DataObjectMock();
    dataObject.initializeForInsert();
    dataObject.title.changeValue("Required Only");
    dataObject.commit(connection);

    ResultSet rs = getRow(dataObject.id.getValue());
    rs.next();
    int kernelsValue = rs.getInt("kernels");
    // kernels has a default value of 0, not null
    assertThat(kernelsValue).as("Nullable field should have default value").isEqualTo(0);

    // Update to null
    DataObjectMock updateObject = new DataObjectMock();
    ResultSet rsUpdate = getRow(dataObject.id.getValue());
    rsUpdate.next();
    updateObject.initializeFromDBObject(rsUpdate);
    updateObject.kernels.changeValue(42);
    updateObject.commit(connection);

    updateObject.kernels.changeValue((Integer) null);
    updateObject.commit(connection);

    ResultSet rsNull = getRow(dataObject.id.getValue());
    rsNull.next();
    rsNull.getInt("kernels");
    assertThat(rsNull.wasNull()).isTrue();
  }

  @Test
  public void testMultipleRecordsConcurrency() throws SQLException {
    DataObjectMock[] objects = new DataObjectMock[5];

    // Insert multiple records
    for (int i = 0; i < 5; i++) {
      objects[i] = new DataObjectMock();
      objects[i].initializeForInsert();
      objects[i].title.changeValue("Record " + i);
      objects[i].kernels.changeValue(i * 10);
      objects[i].commit(connection);
    }

    // Verify unique IDs
    for (int i = 0; i < 5; i++) {
      for (int j = i + 1; j < 5; j++) {
        assertThat(objects[i].id.getValue()).isNotEqualTo(objects[j].id.getValue());
      }
    }

    // Verify all exist
    for (int i = 0; i < 5; i++) {
      ResultSet rs = getRow(objects[i].id.getValue());
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("title")).isEqualTo("Record " + i);
    }
  }

  @Test
  public void testTimestampAutoPopulation() throws SQLException {
    DataObjectMock dataObject = new DataObjectMock();
    dataObject.initializeForInsert();
    dataObject.title.changeValue("Timestamp Test");
    dataObject.commit(connection);

    ResultSet rs = getRow(dataObject.id.getValue());
    rs.next();
    Timestamp dateAdded = rs.getTimestamp("date_added");
    assertThat(dateAdded).isNotNull();

    // Verify recent timestamp (within last minute)
    long diff = System.currentTimeMillis() - dateAdded.getTime();
    assertThat(diff).isLessThan(60000);
  }

  @Test
  public void testUpdateWithNoChanges() throws SQLException {
    DataObjectMock dataObject = new DataObjectMock();
    dataObject.initializeForInsert();
    dataObject.title.changeValue("Unchanged");
    dataObject.kernels.changeValue(5);
    dataObject.commit(connection);

    // Read and commit without changes
    DataObjectMock updateObject = new DataObjectMock();
    ResultSet rsUpdate2 = getRow(dataObject.id.getValue());
    rsUpdate2.next();
    updateObject.initializeFromDBObject(rsUpdate2);
    updateObject.commit(connection);

    // Verify data unchanged
    ResultSet rs = getRow(dataObject.id.getValue());
    rs.next();
    assertThat(rs.getString("title")).isEqualTo("Unchanged");
    assertThat(rs.getInt("kernels")).isEqualTo(5);
  }

  @Test
  public void testSequentialUpdates() throws SQLException {
    DataObjectMock dataObject = new DataObjectMock();
    dataObject.initializeForInsert();
    dataObject.title.changeValue("Version 1");
    dataObject.commit(connection);

    Integer id = dataObject.id.getValue();

    // Multiple sequential updates
    for (int i = 2; i <= 5; i++) {
      DataObjectMock update = new DataObjectMock();
      ResultSet rsLoop = getRow(id);
      rsLoop.next();
      update.initializeFromDBObject(rsLoop);
      update.title.changeValue("Version " + i);
      update.commit(connection);
    }

    // Verify final state
    ResultSet rs = getRow(id);
    rs.next();
    assertThat(rs.getString("title")).isEqualTo("Version 5");
  }
}
