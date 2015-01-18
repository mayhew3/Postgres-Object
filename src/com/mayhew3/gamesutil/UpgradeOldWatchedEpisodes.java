package com.mayhew3.gamesutil;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.net.UnknownHostException;

public class UpgradeOldWatchedEpisodes extends TVDatabaseUtility {

  public UpgradeOldWatchedEpisodes() throws UnknownHostException {
    super("tv");

  }

  // declare mappings from old -> new column names
  // utility should copy the values over, and delete the old field.
  public static void main(String[] args) {
    try {
      UpgradeOldWatchedEpisodes tiVoEpisodes = new UpgradeOldWatchedEpisodes();
      tiVoEpisodes.upgradeRows();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

  }

  public void upgradeRows() {
    DBCursor serieses = _db.getCollection("series")
        .find(new BasicDBObject()
                .append("UnwatchedEpisodes", new BasicDBObject("$gt", 0))
                .append("tvdbEpisodes", new BasicDBObject("$exists", true))
        );

    while(serieses.hasNext()) {

      DBObject series = serieses.next();
      Object seriesTitle = series.get("TiVoSeriesTitle");

      BasicDBList tvdbEpisodes = (BasicDBList) series.get("tvdbEpisodes");
      if (tvdbEpisodes != null) {

        for (Object tvdbEpisode : tvdbEpisodes) {
          DBObject episodeObj = (DBObject) tvdbEpisode;
          Object watched = episodeObj.get("Watched");

          if (Boolean.TRUE.equals(watched)) {
            Object watchedDate = episodeObj.get("WatchedDate");
            Object tiVoProgramId = episodeObj.get("TiVoProgramId");

            BasicDBObject queryObject = new BasicDBObject("TiVoProgramId", tiVoProgramId);
            BasicDBObject updateObject = new BasicDBObject()
                .append("Watched", true);

            if (watchedDate != null) {
              updateObject
                  .append("WatchedDate", watchedDate);
            }

            updateCollectionWithQuery("episodes", queryObject, updateObject);
          }
        }
      }

    }

  }


}
