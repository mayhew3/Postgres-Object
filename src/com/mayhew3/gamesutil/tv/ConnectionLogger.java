package com.mayhew3.gamesutil.tv;

import com.mongodb.*;

import java.net.UnknownHostException;
import java.util.Date;

public class ConnectionLogger extends TVDatabaseUtility {

  private Integer connectionID;

  public ConnectionLogger() throws UnknownHostException {
    super("tv");

    connectionID = findMaximumConnectionId() + 1;
  }


  private Integer findMaximumConnectionId() {
    DBCollection connectlogs = _db.getCollection("connectlogs");
    DBCursor orderedCursor = connectlogs.find().sort(new BasicDBObject("ConnectionID", -1));
    if (orderedCursor.hasNext()) {
      DBObject maxRow = orderedCursor.next();
      return (Integer) maxRow.get("ConnectionID");
    } else {
      return 0;
    }
  }

  public void logConnectionStart(Boolean lookAtAllShows) {
    DBCollection collection = _db.getCollection("connectlogs");
    BasicDBObject basicDBObject = new BasicDBObject()
        .append("StartTime", new Date())
        .append("ConnectionID", connectionID)
        .append("FastUpdate", !lookAtAllShows);

    try {
      collection.insert(basicDBObject);
    } catch (MongoException e) {
      throw new RuntimeException("Error inserting log into database.\r\n" + e.getLocalizedMessage());
    }
  }

  public void logConnectionEnd(BasicDBObject sessionInfo) {
    DBCollection collection = _db.getCollection("connectlogs");

    DBObject existing = findSingleMatch(collection, "ConnectionID", connectionID);

    if (existing == null) {
      throw new RuntimeException("Unable to find connect log with ID " + connectionID);
    }

    Date startTime = (Date) existing.get("StartTime");
    Date endTime = new Date();

    long diffInMillis = endTime.getTime() - startTime.getTime();

    long diffInSeconds = diffInMillis/1000;

    sessionInfo
        .append("EndTime", endTime)
        .append("TimeConnected", diffInSeconds);

    collection.update(new BasicDBObject("ConnectionID", connectionID), new BasicDBObject("$set", sessionInfo));
  }

}
