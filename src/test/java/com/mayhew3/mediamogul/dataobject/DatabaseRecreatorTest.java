package com.mayhew3.mediamogul.dataobject;

import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.model.tv.TVSchema;
import org.junit.Test;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.fail;

public class DatabaseRecreatorTest {

  @Test
  public void testRecreateTestDatabase() throws URISyntaxException, SQLException {
    SQLConnection connection = PostgresConnectionFactory.getSqlConnection(PostgresConnectionFactory.TEST);
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
/*
  @Test
  public void testRecreateDemoDatabase() throws URISyntaxException, SQLException {
    SQLConnection connection = new PostgresConnectionFactory().createConnection("demo");
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
  */
}