package com.mayhew3.gamesutil;

import com.mayhew3.gamesutil.mediaobjectmongo.Series;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.net.UnknownHostException;

public class TVDBUpdateRunner extends TVDatabaseUtility {

  private Integer seriesUpdates = 0;
  private Integer episodesAdded = 0;
  private Integer episodesUpdated = 0;

  public TVDBUpdateRunner() throws UnknownHostException {
    super("tv");
  }

  public static void main(String[] args) {
    try {
      TVDBUpdateRunner tvdbUpdateRunner = new TVDBUpdateRunner();
      tvdbUpdateRunner.runUpdate();
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
        .append("IsSuggestion", new BasicDBObject("$ne", true))
        .append("IgnoreTVDB", new BasicDBObject("$ne", true))
//        .append("SeriesId", new BasicDBObject("$exists", true))
//        .append("SeriesTitle", "MI-5")
//        .append("Tier", 1)
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
      } catch (RuntimeException | ShowFailedException e) {
        e.printStackTrace();
        debug("Show failed: " + show.get("SeriesTitle"));
      }

      debug(i + " out of " + totalRows + " processed.");
    }
  }

  private void updateShow(DBObject show) throws ShowFailedException {
    Series series = new Series();
    series.initializeFromDBObject(show);

    MetacriticUpdater metacriticUpdater = new MetacriticUpdater(_mongoClient, _db, series);
    metacriticUpdater.runUpdater();

    TVDBSeriesUpdater updater = new TVDBSeriesUpdater(_mongoClient, _db, series);
    updater.updateSeries();
    seriesUpdates++;
    episodesAdded += updater.getEpisodesAdded();
    episodesUpdated += updater.getEpisodesUpdated();

  }


  public BasicDBObject getSessionInfo() {
    return new BasicDBObject()
    .append("TVDBSeriesUpdates", seriesUpdates)
    .append("TVDBEpisodesUpdated", episodesUpdated)
    .append("TVDBEpisodesAdded", episodesAdded);
  }
}

