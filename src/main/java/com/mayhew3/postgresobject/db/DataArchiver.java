package com.mayhew3.postgresobject.db;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mayhew3.postgresobject.dataobject.DataObject;
import com.mayhew3.postgresobject.dataobject.FieldValue;
import com.mayhew3.postgresobject.dataobject.FieldValueTimestamp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unused"})
public class DataArchiver {

  private SQLConnection connection;
  private String dbIdentifier;

  // Map of DB table to months of data to keep.
  private List<ArchiveableFactory> tablesToArchive;
  private String logDirectory;

  private static Logger logger = LogManager.getLogger(DataArchiver.class);

  public DataArchiver(SQLConnection connection,
                      String dbIdentifier,
                      String logDirectory,
                      List<ArchiveableFactory> factories) {
    this.connection = connection;
    this.dbIdentifier = dbIdentifier;
    this.logDirectory = logDirectory;
    this.tablesToArchive = factories;
  }

  public void runUpdate() throws SQLException, IOException {
    for (ArchiveableFactory factory : tablesToArchive) {
      runUpdateOnTable(factory);
    }
  }

  private void runUpdateOnTable(ArchiveableFactory factory) throws SQLException, IOException {
    String tableName = factory.tableName();
    String dateColumnName = factory.dateColumnName();
    Integer monthsToKeep = factory.monthsToKeep();

    String otherColumnName = factory.otherColumnName();
    Object otherColumnValue = factory.otherColumnValue();

    File mostRecentFile = null;
    DateTime mostRecentDate = null;
    PrintStream mostRecentStream;

    ResultSet resultSet = monthsToKeep == null ?
        getResultSetForOther(tableName, otherColumnName, otherColumnValue) :
        getResultSetForDates(tableName, dateColumnName, monthsToKeep);

    Integer i = 0;
    while (resultSet.next()) {
      DataObject dataObject = factory.createEntity();
      dataObject.initializeFromDBObject(resultSet);

      if (monthsToKeep != null) {
        FieldValueTimestamp dateValue = (FieldValueTimestamp) dataObject.getFieldValueWithName(dateColumnName);
        assert dateValue != null;

        Timestamp rowTimestamp = dateValue.getValue();
        DateTime rowDateTime = new DateTime(rowTimestamp);

        if (mostRecentDate == null || DateTimeComparator.getDateOnlyInstance().compare(mostRecentDate, rowDateTime) < 0) {
          mostRecentDate = rowDateTime;
          mostRecentFile = getDateBasedFile(tableName, rowTimestamp);
        }
      } else {
        mostRecentFile = getColumnBasedFile(tableName, otherColumnValue);
      }

      mostRecentStream = createValidStream(mostRecentFile, dataObject);

      // todo: Check for duplicate
      copyRowToArchiveFile(dataObject, mostRecentStream);
      deleteOldData(factory, dataObject.id.getValue());
      i++;

      if (i % 100 == 0) {
        debug(i + " rows processed.");
      }
    }

    logger.info(i + " rows processed. Done with table " + tableName);
  }

  private PrintStream createValidStream(File file, DataObject dataObject) throws IOException {
    if (file.exists()) {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
      String firstLine = bufferedReader.readLine();
      if (firstLine != null) {
        validateHeaderRow(dataObject, firstLine);
      }
      return new PrintStream(new FileOutputStream(file, true));
    } else {
      List<String> fieldNames = getFieldNames(dataObject);
      String headerRow = Joiner.on(",").join(fieldNames);
      PrintStream printStream = new PrintStream(new FileOutputStream(file, true));
      printStream.println(headerRow);
      return printStream;
    }
  }

  private ResultSet getResultSetForDates(String tableName, String dateColumnName, Integer monthsToKeep) throws SQLException {
    DateTime today = new DateTime();
    DateTime lastDateToKeep = today.minusMonths(monthsToKeep);
    Timestamp lastDateInTimestamp = new Timestamp(lastDateToKeep.toDate().getTime());

    String sql = "SELECT * " +
        " FROM " + tableName +
        " WHERE " + dateColumnName + " IS NOT NULL " +
        " AND " + dateColumnName + " < ? " +
        " ORDER BY " + dateColumnName;

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, lastDateInTimestamp);

    logger.info("Query executed on table " + tableName + " before " + lastDateToKeep + ". Processing rows...");

    return resultSet;
  }

  private ResultSet getResultSetForOther(String tableName, String columnName, Object columnValue) throws SQLException {
    String sql = "SELECT * " +
        " FROM " + tableName +
        " WHERE " + columnName + " IS NOT NULL " +
        " AND " + columnName + " = ? ";
    logger.info("Query executing on table " + tableName + " using " + columnName + " with value '" + columnValue + "'. Processing rows...");
    return connection.prepareAndExecuteStatementFetch(sql, columnValue);
  }

  private void debug(String message) {
    logger.debug(message);
  }

  private void copyRowToArchiveFile(DataObject dataObject, @NotNull PrintStream printStream) {
    List<String> values = dataObject.getAllFieldValuesIncludingId().stream()
        .sorted(Comparator.comparing(FieldValue::getFieldName))
        .map(fieldValue -> fieldValue.getValue() == null ? "" : formatFieldValue(fieldValue))
        .collect(Collectors.toList());

    String valueText = Joiner.on(",").join(values);

    printStream.println(valueText);
  }

  private String formatFieldValue(FieldValue fieldValue) {
    if (fieldValue.getValue() instanceof String) {
      return "\"" + fieldValue.getValue() + "\"";
    } else {
      return fieldValue.getValue().toString();
    }
  }

  private void validateHeaderRow(DataObject dataObject, String existingHeader) {
    List<String> headerValues = Lists.newArrayList(existingHeader.split(","));
    List<String> expectedValues = getFieldNames(dataObject);

    if (!expectedValues.equals(headerValues)) {
      throw new RuntimeException("Header row on existing file doesn't match data model of " + dataObject.getTableName() + ". " +
          "Expected: " + expectedValues + ", " +
          "Actual: " + headerValues);
    }
  }

  private List<String> getFieldNames(DataObject dataObject) {
    return dataObject.getAllFieldValuesIncludingId()
        .stream()
        .map(FieldValue::getFieldName)
        .sorted()
        .collect(Collectors.toList());
  }

  private void deleteOldData(ArchiveableFactory factory, Integer rowId) throws SQLException {
    String sql = "DELETE FROM " + factory.tableName() +
        " WHERE ID = ? ";
    connection.prepareAndExecuteStatementUpdate(sql, rowId);
  }

  private File getColumnBasedFile(String tableName, Object fileBase) {
    File directory = new File(logDirectory + "\\" + dbIdentifier);
    if (!directory.exists()) {
      //noinspection ResultOfMethodCallIgnored
      directory.mkdir();
    }

    return new File(logDirectory + "\\" + dbIdentifier + "\\Archive_" + tableName + "_" + fileBase + ".csv");
  }

  @NotNull
  private File getDateBasedFile(String tableName, Timestamp rowDate) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
    String dateFormatted = simpleDateFormat.format(rowDate);

    File directory = new File(logDirectory + "\\" + dbIdentifier);
    if (!directory.exists()) {
      //noinspection ResultOfMethodCallIgnored
      directory.mkdir();
    }

    return new File(logDirectory + "\\" + dbIdentifier + "\\Archive_" + tableName + "_" + dateFormatted + ".csv");
  }

}
