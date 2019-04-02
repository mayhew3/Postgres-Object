package com.mayhew3.postgresobject;

import com.mayhew3.postgresobject.dataobject.DataSchemaMock;
import com.mayhew3.postgresobject.dataobject.DatabaseRecreator;
import com.mayhew3.postgresobject.db.PostgresConnectionFactory;
import com.mayhew3.postgresobject.db.SQLConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;

import java.net.URISyntaxException;
import java.sql.SQLException;


public abstract class DatabaseTest {
  protected SQLConnection connection;

  private static Logger logger = LogManager.getLogger(DatabaseTest.class);
  
  @Before
  public void setUp() throws URISyntaxException, SQLException {
    debug("Setting up test DB...");
    connection = PostgresConnectionFactory.getSqlConnection(PostgresConnectionFactory.TEST);
    new DatabaseRecreator(connection).recreateDatabase(DataSchemaMock.schema);
    debug("DB re-created.");
  }

  void debug(Object message) {
    logger.debug(message);
  }
}
