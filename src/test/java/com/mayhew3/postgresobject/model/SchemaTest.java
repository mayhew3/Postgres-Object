package com.mayhew3.postgresobject.model;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.dataobject.DataObjectMismatch;
import com.mayhew3.postgresobject.dataobject.DataSchema;
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

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class SchemaTest {

  private static Logger logger = LogManager.getLogger(SchemaTest.class);

  public abstract DataSchema getDataSchema();

  @Test
  public void testSchemaUpToDate() throws URISyntaxException, SQLException, MissingEnvException {
    String database_url = EnvironmentChecker.getOrThrow("DATABASE_URL");
    logger.debug("Database URL: " + database_url);
    SQLConnection connection = PostgresConnectionFactory.initiateDBConnect(database_url);
    List<DataObjectMismatch> mismatches = getDataSchema().validateSchemaAgainstDatabase(connection);

    if (!mismatches.isEmpty()) {
      logger.error("Mismatches found: ");
      for (DataObjectMismatch mismatch : mismatches) {
        logger.error(" - " + mismatch);
        if (mismatch.getMessage().equals("Table not found!")) {
          logger.error("    - " + mismatch.getDataObject().generateTableCreateStatement());
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