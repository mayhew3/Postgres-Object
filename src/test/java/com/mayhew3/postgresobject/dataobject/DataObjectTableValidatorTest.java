package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.DatabaseTest;
import com.mayhew3.postgresobject.db.DatabaseEnvironment;
import com.mayhew3.postgresobject.db.InternalDatabaseEnvironments;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public class DataObjectTableValidatorTest extends DatabaseTest {

  private static final Logger logger = LogManager.getLogger(DataObjectTableValidatorTest.class);

  @Override
  public DatabaseEnvironment getTestEnvironment() {
    return InternalDatabaseEnvironments.test;
  }

  @Test
  public void testTestSchemaHasNoMismatches() throws SQLException {
    List<DataObjectMismatch> mismatches = DataSchemaMock.schemaDef.validateSchemaAgainstDatabase(connection);

    if (!mismatches.isEmpty()) {
      logger.error("Mismatches found: ");
      for (DataObjectMismatch mismatch : mismatches) {
        logger.error(" - " + mismatch);
        if (mismatch.getMessage().equals("Table not found!")) {
          logger.error("    - " + mismatch.getDataObject().generateTableCreateStatement(connection.getDatabaseType()));
        }
        if (mismatch.getMessage().equals("ForeignKey restraint not found in DB.")) {
          List<String> stringList = mismatch.getDataObject().generateAddForeignKeyStatements();
          for (String fkStatement : stringList) {
            logger.error("    - " + fkStatement);
          }
        }
      }
      fail();
    }

  }


}
