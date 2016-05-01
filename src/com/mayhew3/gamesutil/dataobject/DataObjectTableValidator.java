package com.mayhew3.gamesutil.dataobject;

import com.google.common.base.Joiner;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.TVDBEpisode;
import com.sun.istack.internal.Nullable;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    matchFields();

    return mismatches;
  }

  private void matchFields() throws SQLException {
    List<FieldValue> unfoundFieldValues = dataObject.getAllFieldValues();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM information_schema.columns " +
            "WHERE table_schema = ? " +
            "AND table_name = ? ",
        "public", dataObject.getTableName()
    );

    while (resultSet.next()) {
      String column_name = resultSet.getString("column_name");
      FieldValue fieldValue = dataObject.getFieldValueWithName(column_name);

      if (fieldValue == null) {
        mismatches.add("DB column '" + column_name + "' specified in DB, but not found on " + dataObject.getTableName() + " DataObject.");
      } else {
        String column_default = resultSet.getString("column_default");
        Boolean is_nullable = resultSet.getString("is_nullable").equals("YES");
        String data_type = resultSet.getString("data_type");

        if (!matchesIgnoreCase(column_default, fieldValue.getDefaultValue())) {
          mismatches.add("Column " + fieldValue.getFieldName() + " mismatch on DEFAULT: '" + column_default + "' in DB, '" + fieldValue.getDefaultValue() + "' in DataObject.");
        }
        if (!is_nullable.equals(fieldValue.nullability.getAllowNulls())) {
          mismatches.add("Column " + fieldValue.getFieldName() + " mismatch on is_nullable: '" + is_nullable + "' in DB, '" + fieldValue.nullability.getAllowNulls() + "' in DataObject.");
        }
        if (!matchesIgnoreCase(data_type, fieldValue.getInformationSchemaType())) {
          mismatches.add("Column " + fieldValue.getFieldName() + " mismatch on data_type: '" + data_type + "' in DB, '" + fieldValue.getDDLType() + "' in DataObject.");
        }

        unfoundFieldValues.remove(fieldValue);
      }
    }

    if (!unfoundFieldValues.isEmpty()) {
      List<String> fieldNames = unfoundFieldValues.stream().map(FieldValue::getFieldName).collect(Collectors.toList());
      mismatches.add("FieldValues with no DB columns: " + Joiner.on(", ").join(fieldNames));
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
