package com.mayhew3.gamesutil;

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

      updater.updateFields();
      updater.closeDatabase();
    } catch (UnknownHostException | RuntimeException e) {

      e.printStackTrace();
    }

  }

  public void updateFields() {
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

      debug(i + " out of " + totalRows + " processed.");
    }
  }


  private void updateShow(DBObject show) {
    ObjectId seriesId = (ObjectId) show.get("_id");
    String seriesTitle = (String) show.get("SeriesTitle");
    String tivoId = (String) show.get("SeriesId");

//    updateIsSuggestion(seriesId, seriesTitle, tivoId);
//    updateTiVoName(seriesId, seriesTitle, tivoId, show.get("TiVoName"));

    updateEpisodeCounts(show);
  }

  private void updateEpisodeCounts(DBObject series) {
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

    Object seriesId = series.get("_id");

    DBCollection episodesCollection = _db.getCollection("episodes");
    DBCursor cursor = episodesCollection.find(new BasicDBObject("SeriesId", seriesId));

    while (cursor.hasNext()) {
      DBObject episode = cursor.next();

      Object onTiVo = episode.get("OnTiVo");
      Object suggestion = episode.get("TiVoSuggestion");
      Object showingStartTime = episode.get("TiVoShowingStartTime");
      Object deletedDate = episode.get("TiVoDeletedDate");
      Object watched = episode.get("Watched");
      Object tvdbId = episode.get("tvdbEpisodeId");

      if (Boolean.TRUE.equals(onTiVo)) {


        if (tvdbId == null) {
          unmatchedEpisodes++;
        } else {
          matchedEpisodes++;

          Date showingStartTimeDate = (Date) showingStartTime;
          if (deletedDate == null) {
            activeEpisodes++;

            if (Boolean.TRUE.equals(watched)) {
              watchedEpisodes++;
            } else {
              unwatchedEpisodes++;

              if (lastUnwatched == null || lastUnwatched.before(showingStartTimeDate)) {
                lastUnwatched = showingStartTimeDate;
              }
            }

            if (mostRecent == null || mostRecent.before(showingStartTimeDate)) {
              mostRecent = showingStartTimeDate;
            }
          } else {
            deletedEpisodes++;
          }
          if (Boolean.TRUE.equals(suggestion)) {
            suggestionEpisodes++;
          }
        }

      } else {
        if (tvdbId != null) {
          tvdbOnly++;
          if (!Boolean.TRUE.equals(watched)) {
            unwatchedUnrecorded++;
          }
        }
      }
    }

    BasicDBObject updateObject = new BasicDBObject()
        .append("ActiveEpisodes", activeEpisodes)
        .append("DeletedEpisodes", deletedEpisodes)
        .append("SuggestionEpisodes", suggestionEpisodes)
        .append("UnmatchedEpisodes", unmatchedEpisodes)
        .append("WatchedEpisodes", watchedEpisodes)
        .append("UnwatchedEpisodes", unwatchedEpisodes)
        .append("UnwatchedUnrecorded", unwatchedUnrecorded)
        .append("tvdbOnlyEpisodes", tvdbOnly)
        .append("MatchedEpisodes", matchedEpisodes)
        .append("LastUnwatched", (lastUnwatched == null) ? null : lastUnwatched)
        .append("MostRecent", (mostRecent == null) ? null : mostRecent)
        ;

    updateObjectWithId("series", seriesId, updateObject);
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

