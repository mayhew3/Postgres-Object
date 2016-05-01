package com.mayhew3.gamesutil.dataobject;

import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.TVDBEpisode;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataObjectTableValidator {
  private DataObject dataObject;
  private SQLConnection connection;

  private List<DataObjectMismatch> mismatches;

  DataObjectTableValidator(DataObject dataObject, SQLConnection connection) {
    this.dataObject = dataObject;
    this.connection = connection;

    mismatches = new ArrayList<>();
  }

  // todo: remove. just for testing purposes.
  public static void main(String... args) throws URISyntaxException, SQLException {
    TVDBEpisode episode = new TVDBEpisode();
    SQLConnection connection = new PostgresConnectionFactory().createConnection("test");
    List<DataObjectMismatch> results = new DataObjectTableValidator(episode, connection).matchSchema();

    if (results.isEmpty()) {
      debug("Table " + episode.getTableName() + " checks out!");
    } else {
      debug("Issues found for table " + episode.getTableName() + ":");
      for (DataObjectMismatch result : results) {
        debug(" - " + result);
      }
    }
  }

  List<DataObjectMismatch> matchSchema() throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT COUNT(1) as num_tables " +
            "FROM information_schema.tables " +
            "WHERE table_schema = ? " +
            "AND table_name = ? ",
        "public", dataObject.getTableName()
    );
    resultSet.next();
    if (resultSet.getInt("num_tables") != 1) {
      addMismatch("Table not found!");
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
        addMismatch("DB column '" + column_name + "' specified in DB, but not found.");
      } else {
        String column_default = resultSet.getString("column_default");
        Boolean is_nullable = resultSet.getString("is_nullable").equals("YES");
        String data_type = resultSet.getString("data_type");

        if (!matchesIgnoreCase(column_default, fieldValue.getInformationSchemaDefault())) {
          addMismatch(fieldValue, "DEFAULT mismatch: DB value: " + column_default + ", Field value: " + fieldValue.getInformationSchemaDefault());
        }
        if (!is_nullable.equals(fieldValue.nullability.getAllowNulls())) {
          addMismatch(fieldValue, "is_nullable mismatch: DB value: " + is_nullable + ", Field value: " + fieldValue.nullability.getAllowNulls());
        }
        if (!matchesIgnoreCase(data_type, fieldValue.getInformationSchemaType())) {
          addMismatch(fieldValue, "data_type mismatch: DB value: " + data_type + ", Field value: " + fieldValue.getDDLType());
        }

        unfoundFieldValues.remove(fieldValue);
      }
    }

    if (!unfoundFieldValues.isEmpty()) {
      for (FieldValue fieldValue : unfoundFieldValues) {
        addMismatch(fieldValue, "FieldValue not found in DB.");
      }
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

  private void addMismatch(String message) {
    mismatches.add(new DataObjectMismatch(dataObject, null, message));
  }

  private void addMismatch(@NotNull FieldValue fieldValue, String message) {
    mismatches.add(new DataObjectMismatch(dataObject, fieldValue, message));
  }

  private static void debug(String s) {
    System.out.println(s);
  }
}
