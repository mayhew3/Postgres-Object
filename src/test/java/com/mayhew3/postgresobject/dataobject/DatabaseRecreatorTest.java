package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.DatabaseEnvironment;
import com.mayhew3.postgresobject.db.InternalDatabaseEnvironments;
import com.mayhew3.postgresobject.db.PostgresConnectionFactory;
import com.mayhew3.postgresobject.db.SQLConnection;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.fail;

public class DatabaseRecreatorTest {

  private static final Logger logger = LogManager.getLogger(DatabaseRecreatorTest.class);

  @Test
  public void testRecreateTestDatabase() throws URISyntaxException, SQLException, MissingEnvException {
    DatabaseEnvironment databaseEnvironment = InternalDatabaseEnvironments.test;

    SQLConnection connection = PostgresConnectionFactory.createConnection(databaseEnvironment);
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

  void debug(Object message) {
    logger.debug(message);
  }
}