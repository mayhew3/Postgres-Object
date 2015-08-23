package com.mayhew3.gamesutil.upgrade;

import com.mayhew3.gamesutil.TVDatabaseUtility;
import com.mayhew3.gamesutil.mediaobjectmongo.Series;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.net.UnknownHostException;

public class TVSeriesDatatypeUpgrader extends TVDatabaseUtility {

  public TVSeriesDatatypeUpgrader() throws UnknownHostException {
    super("tv");
  }

  public static void main(String[] args) {


    try {

      TVSeriesDatatypeUpgrader updater = new TVSeriesDatatypeUpgrader();

      updater.updateFields();
      updater.closeDatabase();
    } catch (UnknownHostException | RuntimeException e) {

      e.printStackTrace();
    }

  }

  public void updateFields() {
    BasicDBObject query = new BasicDBObject();

    DBCollection serieses = _db.getCollection("series");
    DBCursor cursor = serieses.find(query);

    int totalRows = cursor.count();
    debug(totalRows + " serieses found for update. Starting.");

    int i = 0;

    while (cursor.hasNext()) {
      i++;
      DBObject seriesObj = cursor.next();

      Series series = new Series();
      series.initializeFromDBObject(seriesObj);

      series.markFieldsForUpgrade();
      series.commit(_db);

      debug(series + ": " + i + " out of " + totalRows + " processed.");
    }
  }


}

