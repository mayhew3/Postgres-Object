package com.mayhew3.gamesutil.dataobject;

import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.TVSchema;
import org.junit.Test;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.fail;

public class DatabaseRecreatorTest {

  @Test
  public void testRecreateTestDatabase() throws URISyntaxException, SQLException {
    SQLConnection connection = new PostgresConnectionFactory().createConnection("test");
    new DatabaseRecreator(connection).recreateDatabase(TVSchema.tv_schema);

    List<DataObjectMismatch> mismatches = TVSchema.tv_schema.validateSchemaAgainstDatabase(connection);

    if (!mismatches.isEmpty()) {
      System.out.println("Mismatches found: ");
      for (DataObjectMismatch mismatch : mismatches) {
        System.out.println(" - " + mismatch);
      }
      fail();
    }
  }
}