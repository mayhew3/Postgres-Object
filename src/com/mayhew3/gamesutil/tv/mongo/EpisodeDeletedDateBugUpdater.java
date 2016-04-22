package com.mayhew3.gamesutil.tv.mongo;

import com.mayhew3.gamesutil.dataobject.EpisodeMongo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.joda.time.DateTime;

import java.net.UnknownHostException;

public class EpisodeDeletedDateBugUpdater extends TVDatabaseUtility {

  public EpisodeDeletedDateBugUpdater() throws UnknownHostException {
    super("tv");
  }

  public static void main(String[] args) {


    try {

      EpisodeDeletedDateBugUpdater updater = new EpisodeDeletedDateBugUpdater();

      updater.updateFields();
      updater.closeDatabase();
    } catch (UnknownHostException | RuntimeException e) {

      e.printStackTrace();
    }

  }

  public void updateFields() {
    BasicDBObject query = new BasicDBObject("TiVoDeletedDate",
        new BasicDBObject("$gte", new DateTime(2015, 9, 5, 0, 0, 0).toDate())
            .append("$lt", new DateTime(2015, 9, 7, 0, 0, 0).toDate()));
    DBCollection falselyDeleted = _db.getCollection("episodes");
    DBCursor cursor = falselyDeleted.find(query);

    int totalRows = cursor.count();
    debug(totalRows + " series found for update. Starting.");

    int i = 0;

    while (cursor.hasNext()) {
      i++;
      DBObject episode = cursor.next();

      updateShow(episode);

      debug(i + " out of " + totalRows + " processed.");
    }
  }


  private void updateShow(DBObject episodeObj) {
    EpisodeMongo episode = new EpisodeMongo();
    episode.initializeFromDBObject(episodeObj);

    episode.tivoDeletedDate.changeValue(null);
    episode.commit(_db);
  }

}

