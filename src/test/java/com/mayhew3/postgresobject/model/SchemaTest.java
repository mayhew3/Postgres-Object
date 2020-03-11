package com.mayhew3.postgresobject.model;

import com.mayhew3.postgresobject.dataobject.DataObjectMismatch;
import com.mayhew3.postgresobject.dataobject.DataSchema;
import com.mayhew3.postgresobject.db.SQLConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.fail;

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class SchemaTest {

  private static Logger logger = LogManager.getLogger(SchemaTest.class);

  public abstract DataSchema getDataSchema();

  public abstract SQLConnection createConnection() throws URISyntaxException, SQLException;

  @Test
  public void testSchemaUpToDate() throws URISyntaxException, SQLException {
    SQLConnection connection = createConnection();
    List<DataObjectMismatch> mismatches = getDataSchema().validateSchemaAgainstDatabase(connection);

    if (!mismatches.isEmpty()) {
      logger.error("Mismatches found: ");
      for (DataObjectMismatch mismatch : mismatches) {
        logger.error(" - " + mismatch);
        if (mismatch.getMessage().equals("Table not found!")) {
          logger.error("    - " + mismatch.getDataObject().generateTableCreateStatement(connection));
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