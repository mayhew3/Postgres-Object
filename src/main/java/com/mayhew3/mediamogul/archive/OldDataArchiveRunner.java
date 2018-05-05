package com.mayhew3.mediamogul.archive;

import com.mayhew3.mediamogul.ArgumentChecker;
import com.mayhew3.mediamogul.dataobject.DataObject;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.scheduler.UpdateRunner;
import com.mayhew3.mediamogul.tv.helper.UpdateMode;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OldDataArchiveRunner implements UpdateRunner {

  private SQLConnection connection;

  // Map of DB table to months of data to keep.
  private List<ArchiveableFactory> tablesToArchive;


  public static void main(String... args) throws URISyntaxException, SQLException {
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
  public void runUpdate() throws SQLException {
    for (ArchiveableFactory factory : tablesToArchive) {
      runUpdateOnTable(factory);
    }
  }

  private void runUpdateOnTable(ArchiveableFactory factory) throws SQLException {
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

      copyRowToArchiveFile(dataObject);
      i++;
    }

    debug(i + " rows processed. Deleting from table...");

    deleteOldData(factory);

    debug("Done.");
  }

  private void debug(String str) {
    System.out.println(new Date() + ": " + str);
  }

  private void copyRowToArchiveFile(DataObject dataObject) {

  }

  private void deleteOldData(ArchiveableFactory factory) {

  }
}
