package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.model.tv.ConnectLog;
import com.mayhew3.gamesutil.db.SQLConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

public class ConnectionLogger {

  private SQLConnection connection;
  private Integer connectionID;

  public ConnectionLogger(SQLConnection connection) {
    this.connection = connection;
  }

  public void initialize() throws SQLException {
    connectionID = findMaximumConnectionId() + 1;
  }

  private Integer findMaximumConnectionId() throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch("SELECT MAX(connection_id) AS max_id FROM connect_log");

    if (resultSet.next()) {
      return resultSet.getInt("max_id");
    }

    return 0;
  }

  public void logConnectionStart(Boolean lookAtAllShows) throws SQLException {
    if (connectionID == null) {
      initialize();
    }

    ConnectLog connectLog = new ConnectLog();
    connectLog.initializeForInsert();

    connectLog.startTime.changeValue(new Date());
    connectLog.connectionID.changeValue(connectionID);
    connectLog.fastUpdate.changeValue(!lookAtAllShows);

    connectLog.commit(connection);
  }

  public void logConnectionEnd() throws SQLException {
    if (connectionID == null) {
      initialize();
    }

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch("SELECT * FROM connect_log WHERE connection_id = ?", connectionID);

    if (resultSet.next()) {
      ConnectLog connectLog = new ConnectLog();
      connectLog.initializeFromDBObject(resultSet);

      Timestamp startTime = connectLog.startTime.getValue();
      Timestamp endTime = new Timestamp(new Date().getTime());

      long diffInMillis = endTime.getTime() - startTime.getTime();
      long diffInSeconds = diffInMillis / 1000;

      connectLog.endTime.changeValue(endTime);
      connectLog.timeConnected.changeValue(diffInSeconds);
      connectLog.commit(connection);
    } else {
      throw new RuntimeException("Unable to find connect log with ID " + connectionID);
    }

  }
}
