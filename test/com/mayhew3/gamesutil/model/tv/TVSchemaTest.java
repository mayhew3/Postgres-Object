package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.DataObjectMismatch;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.fail;

public class TVSchemaTest {

  @Before
  public void setUp() throws Exception {

  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testLocalSchemaUpToDate() throws URISyntaxException, SQLException {
    SQLConnection connection = new PostgresConnectionFactory().createConnection("local");
    List<DataObjectMismatch> mismatches = TVSchema.tv_schema.validateSchemaAgainstDatabase(connection);

    if (!mismatches.isEmpty()) {
      System.out.println("Mismatches found: ");
      for (DataObjectMismatch mismatch : mismatches) {
        System.out.println(" - " + mismatch);
      }
      fail();
    }

  }

  @Test
  public void testTestSchemaUpToDate() throws URISyntaxException, SQLException {
    SQLConnection connection = new PostgresConnectionFactory().createConnection("test");
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