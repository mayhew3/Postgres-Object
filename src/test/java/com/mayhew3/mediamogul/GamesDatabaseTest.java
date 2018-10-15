package com.mayhew3.mediamogul;

import com.mayhew3.mediamogul.dataobject.DatabaseRecreator;
import com.mayhew3.mediamogul.dataobject.GamesTestSchema;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import org.junit.Before;

import java.net.URISyntaxException;
import java.sql.SQLException;


public abstract class GamesDatabaseTest {
  protected SQLConnection connection;

  @Before
  public void setUp() throws URISyntaxException, SQLException {
    System.out.println("Setting up test DB...");
    connection = PostgresConnectionFactory.getSqlConnection(PostgresConnectionFactory.TEST);
    new DatabaseRecreator(connection).recreateDatabase(GamesTestSchema.games_test_schema);
    System.out.println("DB re-created.");
  }

}
