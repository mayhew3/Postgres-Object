package com.mayhew3.mediamogul.archive;

import com.google.common.base.Joiner;
import com.mayhew3.mediamogul.ArgumentChecker;
import com.mayhew3.mediamogul.dataobject.DataObject;
import com.mayhew3.mediamogul.dataobject.FieldValue;
import com.mayhew3.mediamogul.dataobject.FieldValueTimestamp;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.scheduler.UpdateRunner;
import com.mayhew3.mediamogul.tv.helper.UpdateMode;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OldDataArchiveRunner implements UpdateRunner {

  private SQLConnection connection;

  // Map of DB table to months of data to keep.
  private List<ArchiveableFactory> tablesToArchive;


  public static void main(String... args) throws URISyntaxException, SQLException, FileNotFoundException {
    ArgumentChecker argumentChecker = new ArgumentChecker(args);

    SQLConnection connection = PostgresConnectionFactory.createConnection(argumentChecker);

    OldDataArchiveRunner runner = new OldDataArchiveRunner(connection);
    runner.runUpdate();
  }


  private OldDataArchiveRunner(SQLConnection connection) {
    this.connection = connection;

    tablesToArchive = new ArrayList<>();

    // ADD NEW TABLES TO ARCHIVE HERE!
    tablesToArchive.add(new ConnectLogFactory());
  }

  @Override
  public String getRunnerName() {
    return "Old Data Archive";
  }

  @Override
  public @Nullable UpdateMode getUpdateMode() {
    return null;
  }

  @Override
  public void runUpdate() throws SQLException, FileNotFoundException {
    for (ArchiveableFactory factory : tablesToArchive) {
      runUpdateOnTable(factory);
    }
  }

  private void runUpdateOnTable(ArchiveableFactory factory) throws SQLException, FileNotFoundException {
    String tableName = factory.tableName();
    String dateColumnName = factory.dateColumnName();
    Integer monthsToKeep = factory.monthsToKeep();

    DateTime today = new DateTime();
    DateTime lastDateToKeep = today.minusMonths(monthsToKeep);
    Timestamp lastDateInTimestamp = new Timestamp(lastDateToKeep.toDate().getTime());

    String sql = "SELECT * " +
        " FROM " + tableName +
        " WHERE " + dateColumnName + " < ? ";

    Integer i = 1;

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, lastDateInTimestamp);

    debug("Query executed on table " + tableName + " before " + lastDateToKeep + ". Processing rows...");


    while (resultSet.next()) {
      DataObject dataObject = factory.createEntity();
      dataObject.initializeFromDBObject(resultSet);

      FieldValueTimestamp dateValue = (FieldValueTimestamp) dataObject.getFieldValueWithName(dateColumnName);
      copyRowToArchiveFile(dataObject, tableName, dateValue);
      i++;
    }

    debug(i + " rows processed. Deleting from table...");

    deleteOldData(factory);

    debug("Done.");
  }

  private void debug(String str) {
    System.out.println(new Date() + ": " + str);
  }

  private void copyRowToArchiveFile(DataObject dataObject, String tableName, FieldValueTimestamp dateValue) throws FileNotFoundException {
    PrintStream printStream = openStream(tableName, dateValue.getValue());

    List<String> values = dataObject.getAllFieldValues().stream()
        .sorted(Comparator.comparing(FieldValue::getFieldName))
        .map(fieldValue -> fieldValue.getValue() == null ? "" : fieldValue.getValue().toString())
        .collect(Collectors.toList());

    String valueText = Joiner.on(", ").join(values);

    printStream.println(valueText);
  }

  private void deleteOldData(ArchiveableFactory factory) {

  }


  private PrintStream openStream(String tableName, Timestamp rowDate) throws FileNotFoundException {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
    String dateFormatted = simpleDateFormat.format(rowDate);

    String mediaMogulLogs = System.getenv("MediaMogulArchives");

    File file = new File(mediaMogulLogs + "\\Archive_" + tableName + "_" + dateFormatted + ".csv");
    FileOutputStream fos = new FileOutputStream(file, true);

    return new PrintStream(fos);
  }

}
