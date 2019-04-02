package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.PostgresConnectionFactory;
import com.mayhew3.postgresobject.db.SQLConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.fail;

public class DatabaseRecreatorTest {

  private static Logger logger = LogManager.getLogger(DatabaseRecreatorTest.class);
  
  @Test
  public void testRecreateTestDatabase() throws URISyntaxException, SQLException {
    SQLConnection connection = PostgresConnectionFactory.getSqlConnection(PostgresConnectionFactory.TEST);
    new DatabaseRecreator(connection).recreateDatabase(DataSchemaMock.schema);

    List<DataObjectMismatch> mismatches = DataSchemaMock.schema.validateSchemaAgainstDatabase(connection);

    if (!mismatches.isEmpty()) {
      debug("Mismatches found: ");
      for (DataObjectMismatch mismatch : mismatches) {
        debug(" - " + mismatch);
      }
      fail();
    }
  }
/*
  @Test
  public void testRecreateDemoDatabase() throws URISyntaxException, SQLException {
    SQLConnection connection = new PostgresConnectionFactory().createConnection("demo");
    new DatabaseRecreator(connection).recreateDatabase(TVSchema.schema);

    List<DataObjectMismatch> mismatches = TVSchema.schema.validateSchemaAgainstDatabase(connection);

    if (!mismatches.isEmpty()) {
      debug("Mismatches found: ");
      for (DataObjectMismatch mismatch : mismatches) {
        debug(" - " + mismatch);
      }
      fail();
    }
  }
  */

  void debug(Object message) {
    logger.debug(message);
  }
}