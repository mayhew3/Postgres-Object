package com.mayhew3.gamesutil;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.net.UnknownHostException;

public class TVEpisodeDatatypeUpgrader extends TVDatabaseUtility {

  public TVEpisodeDatatypeUpgrader() throws UnknownHostException {
    super("tv");
  }

  public static void main(String[] args) {


    try {

      TVEpisodeDatatypeUpgrader updater = new TVEpisodeDatatypeUpgrader();

      updater.updateFields();
      updater.closeDatabase();
    } catch (UnknownHostException | RuntimeException e) {

      e.printStackTrace();
    }

  }

  public void updateFields() {
    BasicDBObject query = new BasicDBObject();

    DBCollection episodes = _db.getCollection("episodes");
    DBCursor cursor = episodes.find(query);

    int totalRows = cursor.count();
    debug(totalRows + " episodes found for update. Starting.");

    int i = 0;

    while (cursor.hasNext()) {
      i++;
      DBObject episodeObj = cursor.next();

      Episode episode = new Episode();
      episode.initializeFromDBObject(episodeObj);

      episode.markFieldsForUpgrade();
      episode.commit(_db);

      debug(episode + ": " + i + " out of " + totalRows + " processed.");
    }
  }


}

