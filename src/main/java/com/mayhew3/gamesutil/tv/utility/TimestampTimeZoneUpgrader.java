package com.mayhew3.gamesutil.tv.utility;

import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.dataobject.DataObject;
import com.mayhew3.gamesutil.dataobject.DataSchema;
import com.mayhew3.gamesutil.dataobject.FieldValue;
import com.mayhew3.gamesutil.dataobject.FieldValueTimestamp;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.TVSchema;

import java.net.URISyntaxException;
import java.sql.SQLException;

public class TimestampTimeZoneUpgrader {
  private DataSchema schema;
  private SQLConnection connection;

  private TimestampTimeZoneUpgrader(DataSchema schema, SQLConnection connection) {
    this.schema = schema;
    this.connection = connection;
  }

  public static void main(String... args) throws URISyntaxException, SQLException {
    String dbIdentifier = new ArgumentChecker(args).getDBIdentifier();
    SQLConnection connection = new PostgresConnectionFactory().createConnection(dbIdentifier);

    TimestampTimeZoneUpgrader upgrader = new TimestampTimeZoneUpgrader(TVSchema.tv_schema, connection);
    upgrader.upgradeColumns();
  }

  private void upgradeColumns() throws SQLException {
    for (DataObject table : schema.getAllTables()) {
      for (FieldValue fieldValue : table.getAllFieldValues()) {
        if ("timestamp without time zone".equalsIgnoreCase(fieldValue.getInformationSchemaType())) {
          upgradeColumn((FieldValueTimestamp) fieldValue, table);
        }
      }
    }
  }

  private void upgradeColumn(FieldValueTimestamp fieldValue, DataObject table) throws SQLException {
    debug("Updating {" + table.getTableName() + ", " + fieldValue.getFieldName() + "}");

    String sql =
        "ALTER TABLE " + table.getTableName() + " " +
        "ALTER COLUMN " + fieldValue.getFieldName() + " " +
        "TYPE timestamp with time zone " +
        "USING " + fieldValue.getFieldName() + " AT TIME ZONE 'PST'";
    connection.prepareAndExecuteStatementUpdate(sql);
  }


  protected void debug(Object object) {
    System.out.println(object);
  }

}
