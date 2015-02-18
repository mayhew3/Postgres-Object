package com.mayhew3.gamesutil;

import com.mayhew3.gamesutil.mediaobject.Series;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.net.UnknownHostException;

public class MetacriticUpdateRunner extends TVDatabaseUtility {


  public MetacriticUpdateRunner() throws UnknownHostException {
    super("tv");
  }

  public static void main(String[] args) {
    try {
      MetacriticUpdateRunner updateRunner = new MetacriticUpdateRunner();
      updateRunner.runUpdate();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }

  public void runUpdate() {

    try {
      updateShows();
      closeDatabase();
    } catch (RuntimeException e) {
      closeDatabase();
      throw e;
    }
  }

  private void updateShows() {
    BasicDBObject query = new BasicDBObject()
//        .append("IsSuggestion", new BasicDBObject("$ne", true))
        .append("IgnoreTVDB", new BasicDBObject("$ne", true))
//        .append("SeriesId", new BasicDBObject("$exists", true))
//        .append("SeriesTitle", "MI-5")
//        .append("Tier", 1)
        .append("Metacritic", null)
        .append("IsEpisodic", new BasicDBObject("$ne", false));

    DBCollection untaggedShows = _db.getCollection("series");
    DBCursor cursor = untaggedShows.find(query);

    int totalRows = cursor.count();
    debug(totalRows + " series found for update. Starting.");

    int i = 0;

    while (cursor.hasNext()) {
      i++;
      DBObject show = cursor.next();

      try {
        updateShow(show);
      } catch (RuntimeException e) {
        e.printStackTrace();
        debug("Show failed: " + show.get("SeriesTitle"));
      }

      debug(i + " out of " + totalRows + " processed.");
    }
  }

  private void updateShow(DBObject show) {
    Series series = new Series();
    series.initializeFromDBObject(show);

    MetacriticUpdater metacriticUpdater = new MetacriticUpdater(_mongoClient, _db, series);
    metacriticUpdater.runUpdater();

  }


}

