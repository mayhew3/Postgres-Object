package com.mayhew3.gamesutil;

import com.mayhew3.gamesutil.mediaobject.Episode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TVDatabaseSeriesDenormChecker extends TVDatabaseUtility {

  public TVDatabaseSeriesDenormChecker() throws UnknownHostException {
    super("tv");
  }

  public static void main(String[] args) {
    try {
      TVDatabaseSeriesDenormChecker updater = new TVDatabaseSeriesDenormChecker();

      updater.updateFields();
      updater.closeDatabase();
    } catch (UnknownHostException | RuntimeException e) {

      e.printStackTrace();
    }

  }

  public void updateFields() {
    BasicDBObject query = new BasicDBObject()
        ;

    DBCollection untaggedShows = _db.getCollection("series");
    DBCursor cursor = untaggedShows.find(query);

    int totalRows = cursor.count();
    debug(totalRows + " series found for update. Starting.");

    Map<String, Integer> failureCounts = new HashMap<>();
    int i = 0;

    while (cursor.hasNext()) {
      i++;
      DBObject show = cursor.next();

      checkShow(show, failureCounts);

      debug(i + " out of " + totalRows + " processed.");
    }

    if (failureCounts.isEmpty()) {
      debug("All shows have consistent denorms!");
    } else {
      debug("Inconsistencies!");
      for (String fieldName : failureCounts.keySet()) {
        debug(" - " + fieldName + ": " + failureCounts.get(fieldName) + " errors.");
      }
    }
  }


  private void checkShow(DBObject show, Map<String, Integer> failureCounts) {
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

    Object seriesId = show.get("_id");

    DBCollection episodesCollection = _db.getCollection("episodes");
    DBCursor cursor = episodesCollection.find(new BasicDBObject("SeriesId", seriesId));

    DBObject seriesObject = findSingleMatch("series", "_id", seriesId);

    while (cursor.hasNext()) {
      Episode episode = new Episode();
      episode.initializeFromDBObject(cursor.next());

      Boolean onTiVo = episode.onTiVo.getValue();
      Boolean suggestion = episode.tivoSuggestion.getValue();
      Date showingStartTime = episode.tivoShowingStartTime.getValue();
      Date deletedDate = episode.tivoDeletedDate.getValue();
      Boolean watched = episode.watched.getValue();
      Integer tvdbId = episode.tvdbEpisodeId.getValue();
      Boolean matchingStump = episode.matchedStump.getValue();

      if (!matchingStump) {

        // ACTIVE
        if (onTiVo && deletedDate == null) {
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
        if (onTiVo && deletedDate == null && !watched) {
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
        if (onTiVo && isAfter(lastUnwatched, showingStartTime) && deletedDate == null && !watched) {
          lastUnwatched = showingStartTime;
        }
      }

    }

    Map<String, Object> expectedValues = new HashMap<>();

    expectedValues.put("ActiveEpisodes", activeEpisodes);
    expectedValues.put("DeletedEpisodes", deletedEpisodes);
    expectedValues.put("SuggestionEpisodes", suggestionEpisodes);
    expectedValues.put("UnmatchedEpisodes", unmatchedEpisodes);
    expectedValues.put("WatchedEpisodes", watchedEpisodes);
    expectedValues.put("UnwatchedEpisodes", unwatchedEpisodes);
    expectedValues.put("UnwatchedUnrecorded", unwatchedUnrecorded);
    expectedValues.put("tvdbOnlyEpisodes", tvdbOnly);
    expectedValues.put("MatchedEpisodes", matchedEpisodes);
    expectedValues.put("LastUnwatched", lastUnwatched);
    expectedValues.put("MostRecent", mostRecent);

    for (String fieldName : expectedValues.keySet()) {
      Object foundObject = seriesObject.get(fieldName);
      Object expectedObject = expectedValues.get(fieldName);

      if (!Objects.equals(foundObject, expectedObject)) {
        Integer currentCount = failureCounts.get(fieldName);
        Integer updatedCount = currentCount == null ? 1 : (currentCount + 1);

        failureCounts.put(fieldName, updatedCount);
      }
    }
  }

  private Boolean isAfter(Date trackingDate, Date newDate) {
    return trackingDate == null || trackingDate.before(newDate);
  }


}

