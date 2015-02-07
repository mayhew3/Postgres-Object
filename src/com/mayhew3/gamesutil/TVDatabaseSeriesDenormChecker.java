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
      Boolean suggestion = episode.tiVoSuggestion.getValue();
      Date showingStartTime = episode.tiVoShowingStartTime.getValue();
      Date deletedDate = episode.tiVoDeletedDate.getValue();
      Boolean watched = episode.watched.getValue();
      Integer tvdbId = episode.tvdbEpisodeId.getValue();
      Boolean matchingStump = episode.matchedStump.getValue();

      if (Boolean.TRUE.equals(onTiVo)) {
        if (tvdbId == null) {
          if (!Boolean.TRUE.equals(matchingStump)) {
            unmatchedEpisodes++;
          }
        } else {
          matchedEpisodes++;

          if (deletedDate == null) {
            activeEpisodes++;

            if (Boolean.TRUE.equals(watched)) {
              watchedEpisodes++;
            } else {
              unwatchedEpisodes++;

              if (lastUnwatched == null || lastUnwatched.before(showingStartTime)) {
                lastUnwatched = showingStartTime;
              }
            }

            if (mostRecent == null || mostRecent.before(showingStartTime)) {
              mostRecent = showingStartTime;
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
    expectedValues.put("LastUnwatched", (lastUnwatched == null) ? null : lastUnwatched);
    expectedValues.put("MostRecent", (mostRecent == null) ? null : mostRecent);

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


}

