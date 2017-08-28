package com.mayhew3.mediamogul;

import com.mayhew3.mediamogul.dataobject.DatabaseRecreator;
import com.mayhew3.mediamogul.dataobject.TVTestSchema;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import org.junit.Before;

import java.net.URISyntaxException;
import java.sql.SQLException;


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
