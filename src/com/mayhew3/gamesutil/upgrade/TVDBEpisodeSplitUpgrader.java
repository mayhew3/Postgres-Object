package com.mayhew3.gamesutil.upgrade;

import com.mayhew3.gamesutil.TVDatabaseUtility;
import com.mayhew3.gamesutil.mediaobjectmongo.Episode;
import com.mayhew3.gamesutil.mediaobjectmongo.FieldValue;
import com.mayhew3.gamesutil.mediaobjectmongo.Series;
import com.mayhew3.gamesutil.mediaobjectmongo.TVDBEpisode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;

import java.net.UnknownHostException;

public class TVDBEpisodeSplitUpgrader extends TVDatabaseUtility {

  public TVDBEpisodeSplitUpgrader() throws UnknownHostException {
    super("tv");
  }

  public static void main(String[] args) {


    try {

      TVDBEpisodeSplitUpgrader updater = new TVDBEpisodeSplitUpgrader();

      updater.updateFields();
      updater.closeDatabase();
    } catch (UnknownHostException | RuntimeException e) {

      e.printStackTrace();
    }

  }

  public void updateFields() {
    BasicDBObject query = new BasicDBObject()
        .append("tvdbId", new BasicDBObject("$exists", true));

    DBCollection serieses = _db.getCollection("series");
    DBCursor cursor = serieses.find(query);

    int totalRows = cursor.count();
    debug(totalRows + " shows found for update. Starting.");

    int i = 0;

    while (cursor.hasNext()) {
      i++;
      DBObject seriesObj = cursor.next();

      Series series = new Series();
      series.initializeFromDBObject(seriesObj);

      DBCollection episodes = _db.getCollection("episodes");
      ObjectId tvdbSeriesId = series._id.getValue();

      if (tvdbSeriesId == null) {
        throw new RuntimeException("Null TVDB ID.");
      }

      BasicDBObject episodeQuery = new BasicDBObject()
          .append("SeriesId", tvdbSeriesId)
          .append("tvdbEpisodeId", new BasicDBObject("$exists", true));

      DBCursor episodeCursor = episodes.find(episodeQuery);
      int episodeCount = episodeCursor.count();
      debug(episodeCount + " episodes found for series '" + series.tvdbName.getValue() + "'");

      int episodeIterativeCount = 0;

      // todo: The Graham Norton Show is inserting 283 rows into tvdbEpisodes when there are only 266 episodes
      // todo: Guessing maybe committing to the collection while iterating through it is causing issues?
      // todo: Dump objects into an array an iterate over it instead?

      while (episodeCursor.hasNext()) {
        episodeIterativeCount++;

        DBObject episodeObj = episodeCursor.next();

        Episode episode = new Episode();
        episode.initializeFromDBObject(episodeObj);

        TVDBEpisode tvdbEpisode = new TVDBEpisode();
        tvdbEpisode.initializeForInsert();

        for (FieldValue fieldValue : tvdbEpisode.getAllFieldValues()) {
          FieldValue matchingField = episode.getMatchingField(fieldValue);

          //noinspection unchecked
          fieldValue.changeValue(matchingField.getValue());
        }

        tvdbEpisode.commit(_db);

        series.tvdbEpisodes.addToArray(tvdbEpisode._id.getValue());

        Boolean onTiVo = episode.onTiVo.getValue();
        if (onTiVo == null || !onTiVo) {
          series.episodes.removeFromArray(episode._id.getValue());
          episode.matchingStump.changeValue(true);
          episode.commit(_db);
        }

      }

      if (episodeCount != episodeIterativeCount) {
        int poop = 0;
      }

      debug(series + ": " + i + " out of " + totalRows + " processed.");

      series.commit(_db);
    }
  }


}

