package com.mayhew3.gamesutil;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;

import java.net.UnknownHostException;

public class TVDatabaseSeriesDenormUpdater extends DatabaseUtility {

  public static void main(String[] args) {


    try {

      connect("tv");

      updateFields();
    } catch (UnknownHostException | RuntimeException e) {
      e.printStackTrace();
    } finally {
      closeDatabase();
    }

  }

  public static void updateFields() {
    BasicDBObject query = new BasicDBObject()
        .append("SeriesId", new BasicDBObject("$exists", true))
        ;

    DBCollection untaggedShows = _db.getCollection("series");
    DBCursor cursor = untaggedShows.find(query);

    int totalRows = cursor.size();
    debug(totalRows + " series found for update. Starting.");

    int i = 0;

    while (cursor.hasNext()) {
      i++;
      DBObject show = cursor.next();

      updateShow(show);

      debug(i + " out of " + totalRows + " processed.");
    }
  }


  private static void updateShow(DBObject show) {
    ObjectId seriesId = (ObjectId) show.get("_id");
    String seriesTitle = (String) show.get("SeriesTitle");
    String tivoId = (String) show.get("SeriesId");

    BasicDBObject suggestion = new BasicDBObject()
        .append("SeriesId", tivoId)
        .append("Suggestion", false);

    DBCursor cursor = _db.getCollection("episodes").find(suggestion);

    boolean isIntentional = cursor.hasNext();

    BasicDBObject updateQuery = new BasicDBObject("_id", seriesId);
    BasicDBObject updateChange = new BasicDBObject("IsSuggestion", !isIntentional);

    debug("Updating '" + seriesTitle + "' to IsSuggestion '" + !isIntentional + "'");

    _db.getCollection("series").update(updateQuery, new BasicDBObject("$set", updateChange));
  }

}

