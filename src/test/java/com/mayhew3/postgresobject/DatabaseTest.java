package com.mayhew3.postgresobject;

import com.mayhew3.postgresobject.dataobject.DataSchemaMock;
import com.mayhew3.postgresobject.dataobject.DatabaseRecreator;
import com.mayhew3.postgresobject.db.DatabaseEnvironment;
import com.mayhew3.postgresobject.db.PostgresConnectionFactory;
import com.mayhew3.postgresobject.db.SQLConnection;
import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;

import java.net.URISyntaxException;
import java.sql.SQLException;


public abstract class DatabaseTest {
  protected SQLConnection connection;

  private static final Logger logger = LogManager.getLogger(DatabaseTest.class);

  public abstract DatabaseEnvironment getTestEnvironment();

  @Before
  public void setUp() throws URISyntaxException, SQLException, MissingEnvException {
    debug("Setting up test DB...");
    connection = PostgresConnectionFactory.createConnection(getTestEnvironment());
    new DatabaseRecreator(connection).recreateDatabase(DataSchemaMock.schema);
    debug("DB re-created.");
  }

  void debug(Object message) {
    logger.debug(message);
  }
}
