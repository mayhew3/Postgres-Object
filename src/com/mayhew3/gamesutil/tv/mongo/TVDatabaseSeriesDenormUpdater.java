package com.mayhew3.gamesutil.tv.mongo;

import com.mayhew3.gamesutil.dataobject.mongo.EpisodeMongo;
import com.mayhew3.gamesutil.dataobject.FieldValue;
import com.mayhew3.gamesutil.dataobject.mongo.SeriesMongo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;

import java.net.UnknownHostException;
import java.util.Date;

public class TVDatabaseSeriesDenormUpdater extends TVDatabaseUtility {

  public TVDatabaseSeriesDenormUpdater() throws UnknownHostException {
    super("tv");
  }

  public static void main(String[] args) {


    try {

      TVDatabaseSeriesDenormUpdater updater = new TVDatabaseSeriesDenormUpdater();

      updater.updateFields(true);
      updater.closeDatabase();
    } catch (UnknownHostException | RuntimeException e) {

      e.printStackTrace();
    }

  }

  public void updateFields(Boolean verbose) {
    BasicDBObject query = new BasicDBObject()

//        .append("SeriesId", new BasicDBObject("$exists", true))
        ;

    DBCollection untaggedShows = _db.getCollection("series");
    DBCursor cursor = untaggedShows.find(query);

    int totalRows = cursor.count();
    debug(totalRows + " series found for update. Starting.");

    int i = 0;

    while (cursor.hasNext()) {
      i++;
      DBObject show = cursor.next();

      updateShow(show);

      if (verbose) {
        debug(i + " out of " + totalRows + " processed.");
      }
    }

    debug("Finished updating denorms.");
  }


  private void updateShow(DBObject show) {
    ObjectId seriesId = (ObjectId) show.get("_id");
    String seriesTitle = (String) show.get("SeriesTitle");
    String tivoId = (String) show.get("SeriesId");

//    updateIsSuggestion(seriesId, seriesTitle, tivoId);
//    updateTiVoName(seriesId, seriesTitle, tivoId, show.get("TiVoName"));

    updateEpisodeCounts(show);
  }

  private void updateEpisodeCounts(DBObject seriesObj) {
    Integer activeEpisodes = 0;
    Integer deletedEpisodes = 0;
    Integer suggestionEpisodes = 0;
    Integer unmatchedEpisodes = 0;
    Integer watchedEpisodes = 0;
    Integer unwatchedEpisodes = 0;
    Integer unwatchedUnrecorded = 0;
    Date lastUnwatched = null;
    Date mostRecent = null;

    Integer tvdbOnly = 0;
    Integer matchedEpisodes = 0;

    Object seriesId = seriesObj.get("_id");

    DBCollection episodesCollection = _db.getCollection("episodes");
    DBCursor cursor = episodesCollection.find(new BasicDBObject("SeriesId", seriesId));

    while (cursor.hasNext()) {
      EpisodeMongo episode = new EpisodeMongo();
      episode.initializeFromDBObject(cursor.next());

      Boolean onTiVo = episode.onTiVo.getValue();
      Boolean suggestion = episode.tivoSuggestion.getValue();
      Date showingStartTime = episode.tivoShowingStartTime.getValue();
      Date deletedDate = episode.tivoDeletedDate.getValue();
      Boolean watched = episode.watched.getValue();
      Integer tvdbId = episode.tvdbEpisodeId.getValue();
      Boolean matchingStump = episode.matchingStump.getValue();

      if (!matchingStump) {

        // ACTIVE
        if (onTiVo && !suggestion && deletedDate == null) {
          activeEpisodes++;
        }

        // DELETED
        if (onTiVo && deletedDate != null) {
          deletedEpisodes++;
        }

        // SUGGESTIONS
        if (onTiVo && suggestion && deletedDate == null) {
          suggestionEpisodes++;
        }

        // WATCHED
        if (watched) {
          watchedEpisodes++;
        }

        // UNWATCHED
        if (onTiVo && !suggestion && deletedDate == null && !watched) {
          unwatchedEpisodes++;
        }

        // MATCHED
        if (onTiVo && tvdbId != null) {
          matchedEpisodes++;
        }

        // UNMATCHED
        if (onTiVo && tvdbId == null) {
          unmatchedEpisodes++;
        }

        // TVDB ONLY
        if (!onTiVo && tvdbId != null) {
          tvdbOnly++;
        }

        // UNWATCHED, UNRECORDED
        if (!onTiVo && !watched) {
          unwatchedUnrecorded++;
        }

        // LAST EPISODE
        if (onTiVo && isAfter(mostRecent, showingStartTime) && deletedDate == null) {
          mostRecent = showingStartTime;
        }

        // LAST UNWATCHED EPISODE
        if (onTiVo && isAfter(lastUnwatched, showingStartTime) && !suggestion && deletedDate == null && !watched) {
          lastUnwatched = showingStartTime;
        }
      }
    }

    SeriesMongo series = new SeriesMongo();
    series.initializeFromDBObject(seriesObj);

    series.activeEpisodes.changeValue(activeEpisodes);

    series.deletedEpisodes.changeValue(deletedEpisodes);
    series.suggestionEpisodes.changeValue(suggestionEpisodes);
    series.unmatchedEpisodes.changeValue(unmatchedEpisodes);
    series.watchedEpisodes.changeValue(watchedEpisodes);
    series.unwatchedEpisodes.changeValue(unwatchedEpisodes);
    series.unwatchedUnrecorded.changeValue(unwatchedUnrecorded);
    series.tvdbOnlyEpisodes.changeValue(tvdbOnly);
    series.matchedEpisodes.changeValue(matchedEpisodes);
    series.lastUnwatched.changeValue(lastUnwatched);
    series.mostRecent.changeValue(mostRecent);

    series.commit(_db);

  }


  private Boolean isAfter(Date trackingDate, Date newDate) {
    return trackingDate == null || trackingDate.before(newDate);
  }



  private void incrementValue(FieldValue<Integer> fieldValue) {

  }

  private void updateTiVoName(ObjectId seriesId, String seriesTitle, String tivoId, Object tivoName) {
    if (tivoName == null) {
      BasicDBObject queryObject = new BasicDBObject("_id", seriesId);
      BasicDBObject updateObject = new BasicDBObject("TiVoName", seriesTitle);

      debug("Updating TiVoName.");

      updateCollectionWithQuery("series", queryObject, updateObject);
    }
  }

  private void updateIsSuggestion(ObjectId seriesId, String seriesTitle, String tivoId) {
    boolean hasIntentional = hasIntentionallyRecordedEpisodes(tivoId);
    boolean hasSuggested = hasSuggestedEpisodes(tivoId);

    boolean isSuggestion = hasSuggested && !hasIntentional;

    BasicDBObject updateQuery = new BasicDBObject("_id", seriesId);
    BasicDBObject updateChange = new BasicDBObject("IsSuggestion", isSuggestion);

    debug("Updating '" + seriesTitle + "' to IsSuggestion '" + isSuggestion + "'");

    updateCollectionWithQuery("series", updateQuery, updateChange);
  }

  private boolean hasIntentionallyRecordedEpisodes(String tivoId) {
    BasicDBObject suggestion = new BasicDBObject()
        .append("SeriesId", tivoId)
        .append("Suggestion", false);

    DBCursor cursor = _db.getCollection("episodes").find(suggestion);

    return cursor.hasNext();
  }

  private boolean hasSuggestedEpisodes(String tivoId) {
    BasicDBObject suggestion = new BasicDBObject()
        .append("SeriesId", tivoId)
        .append("Suggestion", true);

    DBCursor cursor = _db.getCollection("episodes").find(suggestion);

    return cursor.hasNext();
  }

}

