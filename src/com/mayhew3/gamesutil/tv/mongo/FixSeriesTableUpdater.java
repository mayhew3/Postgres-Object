package com.mayhew3.gamesutil.tv.mongo;

import com.mongodb.*;

import java.net.UnknownHostException;

public class FixSeriesTableUpdater extends TVDatabaseUtility {


  public FixSeriesTableUpdater() throws UnknownHostException {
    super("tv");

  }

  // declare mappings from old -> new column names
  // utility should copy the values over, and delete the old field.
  public static void main(String[] args) {
    try {
      FixSeriesTableUpdater tiVoEpisodes = new FixSeriesTableUpdater();
      tiVoEpisodes.upgradeRows();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

  }

  public void upgradeRows() {
    DBCollection collection = _db.getCollection("series");
    DBCursor serieses = collection
        .find(new BasicDBObject()
                .append("episodes", new BasicDBObject("$exists", true))
        );

    Integer processed = 0;

    while(serieses.hasNext()) {

      DBObject series = serieses.next();
      BasicDBList episodes = (BasicDBList) series.get("episodes");


      if (episodes.contains(null)) {
        processed++;

        Object seriesTitle = series.get("SeriesTitle");

        debug(processed + ". " + seriesTitle);

        BasicDBObject queryObject = new BasicDBObject("_id", series.get("_id"));
        BasicDBObject updateObject = new BasicDBObject("episodes", null);

        collection.update(queryObject, new BasicDBObject("$pull", updateObject), false, true);
      }


    }

    debug("Completed.");
  }


}
