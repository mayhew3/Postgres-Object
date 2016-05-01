package com.mayhew3.gamesutil.dataobject;

import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.sun.istack.internal.Nullable;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataObjectTableValidator {
  private DataObject dataObject;
  private SQLConnection connection;

  private List<String> mismatches;

  public DataObjectTableValidator(DataObject dataObject, SQLConnection connection) {
    this.dataObject = dataObject;
    this.connection = connection;

    mismatches = new ArrayList<>();
  }

  // todo: remove. just for testing purposes.
  public static void main(String... args) throws URISyntaxException, SQLException {
    TVDBEpisode episode = new TVDBEpisode();
    SQLConnection connection = new PostgresConnectionFactory().createConnection("test");
    List<String> results = new DataObjectTableValidator(episode, connection).matchSchema();

    if (results.isEmpty()) {
      debug("Table " + episode.getTableName() + " checks out!");
    } else {
      debug("Issues found for table " + episode.getTableName() + ":");
      for (String result : results) {
        debug(" - " + result);
      }
    }
  }

  public List<String> matchSchema() throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT COUNT(1) as num_tables " +
            "FROM information_schema.tables " +
            "WHERE table_schema = ? " +
            "AND table_name = ? ",
        "public", dataObject.getTableName()
    );
    resultSet.next();
    if (resultSet.getInt("num_tables") != 1) {
      mismatches.add("Table " + dataObject.getTableName() + " not found.");
      return mismatches;
    }

    for (FieldValue fieldValue : dataObject.getAllFieldValues()) {
      matchField(fieldValue);
    }

    // todo: check if any columns are missing from DataObject

    return mismatches;
  }

  private void matchField(FieldValue fieldValue) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM information_schema.columns " +
            "WHERE table_schema = ? " +
            "AND table_name = ? " +
            "AND column_name = ?",
        "public", dataObject.getTableName(), fieldValue.getFieldName()
    );

    if (resultSet.next()) {
      String column_default = resultSet.getString("column_default");
      Boolean is_nullable = resultSet.getString("is_nullable").equals("YES");
      String data_type = resultSet.getString("data_type");

      if (!matchesIgnoreCase(column_default, fieldValue.getDefaultValue())) {
        mismatches.add("Column " + fieldValue.getFieldName() + " mismatch on DEFAULT: '" + column_default + "' should be '" + fieldValue.getDefaultValue() + "'.");
      }
      if (!is_nullable.equals(fieldValue.nullability.getAllowNulls())) {
        mismatches.add("Column " + fieldValue.getFieldName() + " mismatch on is_nullable: '" + is_nullable + "' should be '" + fieldValue.nullability.getAllowNulls() + "'.");
      }
      if (!matchesIgnoreCase(data_type, fieldValue.getInformationSchemaType())) {
        mismatches.add("Column " + fieldValue.getFieldName() + " mismatch on data_type: '" + data_type + "' should be '" + fieldValue.getDDLType() + "'.");
      }
    } else {
      mismatches.add("Column " + fieldValue.getFieldName() + " specified in DataObject not found in DB schema.");
    }
  }

  @SuppressWarnings("SimplifiableIfStatement")
  private Boolean matchesIgnoreCase(@Nullable String s1, @Nullable String s2) {
    if (s1 == null && s2 == null) {
      return true;
    } else if (s1 != null && s2 != null) {
      return s1.toLowerCase().equals(s2.toLowerCase());
    } else {
      return false;
    }
  }

  private static void debug(String s) {
    System.out.println(s);
  }
}
