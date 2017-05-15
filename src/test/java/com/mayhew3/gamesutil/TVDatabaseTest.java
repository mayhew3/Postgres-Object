package com.mayhew3.gamesutil;

import com.mayhew3.gamesutil.dataobject.DatabaseRecreator;
import com.mayhew3.gamesutil.dataobject.TVTestSchema;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import org.junit.Before;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.sql.Timestamp;


public abstract class TVDatabaseTest {
  protected SQLConnection connection;

  @Before
  public void setUp() throws URISyntaxException, SQLException {
    System.out.println("Setting up test DB...");
    connection = new PostgresConnectionFactory().createConnection("test");
    new DatabaseRecreator(connection).recreateDatabase(TVTestSchema.tv_test_schema);
    System.out.println("DB re-created.");
  }

}
