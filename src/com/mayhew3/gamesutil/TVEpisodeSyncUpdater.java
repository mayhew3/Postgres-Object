package com.mayhew3.gamesutil;

import com.mongodb.*;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import java.net.UnknownHostException;

public class TVEpisodeSyncUpdater extends DatabaseUtility {

  public static void main(String[] args) {
    try {
      connect("tv");
      updateFields();
    } catch (UnknownHostException | RuntimeException e) {
      e.printStackTrace();
    } finally {
      closeDatabase();
    }
  }

  public static void updateFields() {
    BasicDBObject query = new BasicDBObject()
        .append("IsSuggestion", false)
        .append("IsEpisodic", true)
        .append("SeriesId", new BasicDBObject("$exists", true))
//        .append("UnmatchedEpisodes", new BasicDBObject("$ne", 0))
        ;

    DBCollection allSeries = _db.getCollection("series");
    DBCursor cursor = allSeries.find(query);

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


  private static void updateShow(DBObject show) {
    ObjectId seriesId = (ObjectId) show.get("_id");
    String seriesTitle = (String) show.get("SeriesTitle");
    String tivoId = (String) show.get("SeriesId");

    BasicDBList tvdbEpisodes = (BasicDBList) show.get("tvdbEpisodes");

    if (tvdbEpisodes != null && !tvdbEpisodes.isEmpty()) {

      debug("Attempting to match for series '" + seriesTitle + "'");

      BasicDBObject episodeQuery = new BasicDBObject()
          .append("SeriesId", tivoId);

      Integer activeEpisodes = 0;
      Integer deletedEpisodes = 0;
      Integer suggestionEpisodes = 0;
      Integer unwatchedEpisodes = 0;

      DateTime lastUnwatched = null;
      DateTime mostRecent = null;

      Integer notMatched = 0;

      DBCursor cursor = _db.getCollection("episodes").find(episodeQuery);

      int i = 0;

      while (cursor.hasNext()) {
        i++;

        DBObject tivoEpisode = cursor.next();
        Object id = tivoEpisode.get("_id");

        DBObject episodeMatch = findEpisodeMatch(tivoEpisode, tvdbEpisodes);

        if (episodeMatch == null) {
          singleFieldUpdateWithId("episodes", id, "MatchFound", false);
          notMatched++;
        } else {
          Object suggestion = tivoEpisode.get("Suggestion");
          if (Boolean.TRUE.equals(suggestion)) {
            suggestionEpisodes++;
          }

          Object deletedDate = tivoEpisode.get("DeletedDate");
          if (deletedDate == null) {
            activeEpisodes++;
          } else {
            deletedEpisodes++;
          }

          Object showingStartTime = tivoEpisode.get("ShowingStartTime");

          if (showingStartTime != null) {
            DateTime startTime = new DateTime(showingStartTime);
            if (mostRecent == null || startTime.isAfter(mostRecent)) {
              mostRecent = startTime;
            }
          }

          Object watched = tivoEpisode.get("Watched");
          if (!Boolean.TRUE.equals(watched)) {
            watched = Boolean.FALSE;
            unwatchedEpisodes++;
            if (showingStartTime != null) {
              DateTime startTime = new DateTime(showingStartTime);
              if (lastUnwatched == null || startTime.isAfter(lastUnwatched)) {
                lastUnwatched = startTime;
              }
            }
          }

          BasicDBObject queryObject = new BasicDBObject()
              .append("_id", seriesId)
              .append("tvdbEpisodes.tvdbEpisodeId", episodeMatch.get("tvdbEpisodeId"));

          BasicDBObject updateObject = new BasicDBObject()
              .append("tvdbEpisodes.$.OnTiVo", true)
              .append("tvdbEpisodes.$.DateAdded", tivoEpisode.get("AddedDate"))
              .append("tvdbEpisodes.$.TiVoDescription", tivoEpisode.get("Description"))
              .append("tvdbEpisodes.$.TiVoEpisodeTitle", tivoEpisode.get("EpisodeTitle"))
              .append("tvdbEpisodes.$.TiVoEpisodeNumber", tivoEpisode.get("EpisodeNumber"))
              .append("tvdbEpisodes.$.TiVoShowingStartTime", showingStartTime)
              .append("tvdbEpisodes.$.TiVoDeletedDate", deletedDate)
              .append("tvdbEpisodes.$.TiVoHD", tivoEpisode.get("HighDefinition"))
              .append("tvdbEpisodes.$.TiVoProgramId", tivoEpisode.get("ProgramId"))
              .append("tvdbEpisodes.$.TiVoShowingDuration", tivoEpisode.get("ShowingDuration"))
              .append("tvdbEpisodes.$.TiVoChannel", tivoEpisode.get("SourceChannel"))
              .append("tvdbEpisodes.$.TiVoStation", tivoEpisode.get("SourceStation"))
              .append("tvdbEpisodes.$.TiVoSuggestion", suggestion)
              .append("tvdbEpisodes.$.TiVoRating", tivoEpisode.get("TvRating"))
              .append("tvdbEpisodes.$.TiVoUrl", tivoEpisode.get("Url"))
              .append("tvdbEpisodes.$.Watched", watched)
              ;

          updateCollectionWithQuery("series", queryObject, updateObject);
        }
      }

      if (i > 0) {
        BasicDBObject updateObject = new BasicDBObject()
            .append("ActiveEpisodes", activeEpisodes)
            .append("DeletedEpisodes", deletedEpisodes)
            .append("SuggestionEpisodes", suggestionEpisodes)
            .append("UnmatchedEpisodes", notMatched)
            .append("UnwatchedEpisodes", unwatchedEpisodes)
            .append("LastUnwatched", (lastUnwatched == null) ? null : lastUnwatched.toDate())
            .append("MostRecent", (mostRecent == null) ? null : mostRecent.toDate())
            ;

        updateObjectWithId("series", seriesId, updateObject);

        debug(notMatched + "/" + i + " of TiVo episodes were unmatched.");
      } else {
        debug("No TiVo episodes found.");
      }

    } else {
      debug("No TVDB episodes found for series '" + seriesTitle + "'");
    }




  }

  private static DBObject findEpisodeMatch(DBObject tivoEpisode, BasicDBList tvdbEpisodes) {
    String titleObject = (String) tivoEpisode.get("EpisodeTitle");

    if (titleObject != null) {
      for (Object tvdbEpisode : tvdbEpisodes) {
        BasicDBObject fullObject = (BasicDBObject) tvdbEpisode;
        String tvdbTitleObject = (String) fullObject.get("tvdbEpisodeName");

        if (titleObject.equalsIgnoreCase(tvdbTitleObject)) {
          return fullObject;
        }
      }
    }

    // no match found on episode title. Try episode number.

    Object numberObject = tivoEpisode.get("EpisodeNumber");

    if (numberObject != null) {
      Integer episodeNumber = Integer.valueOf((String) numberObject);
      Integer seasonNumber = 1;

      if (episodeNumber < 100) {
        DBObject match = checkForNumberMatch(seasonNumber, episodeNumber, tvdbEpisodes);
        if (match != null) {
          return match;
        }
      } else {
        String episodeNumberString = episodeNumber.toString();
        int seasonLength = episodeNumberString.length() / 2;

        String seasonString = episodeNumberString.substring(0, seasonLength);
        String episodeString = episodeNumberString.substring(seasonLength, episodeNumberString.length());

        DBObject match = checkForNumberMatch(Integer.valueOf(seasonString), Integer.valueOf(episodeString), tvdbEpisodes);

        if (match != null) {
          return match;
        }
      }
    }

    // no match on episode number. Try air date.

    Object startTimeObj = tivoEpisode.get("ShowingStartTime");

    if (startTimeObj != null) {
      DateTime showingStartTime = new DateTime(startTimeObj);

      for (Object tvdbEpisode : tvdbEpisodes) {
        BasicDBObject fullObject = (BasicDBObject) tvdbEpisode;
        Object firstAiredObj = fullObject.get("tvdbFirstAired");

        if (firstAiredObj != null) {
          DateTime firstAired = new DateTime(firstAiredObj);

          DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();

          if (comparator.compare(showingStartTime, firstAired) == 0) {
            return fullObject;
          }
        }
      }

    }

    return null;
  }

  private static DBObject checkForNumberMatch(Integer seasonNumber, Integer episodeNumber, BasicDBList tvdbEpisodes) {
    for (Object tvdbEpisode : tvdbEpisodes) {
      BasicDBObject fullObject = (BasicDBObject) tvdbEpisode;

      String tvdbSeasonStr = (String) fullObject.get("tvdbSeason");
      String tvdbEpisodeNumberStr = (String) fullObject.get("tvdbEpisodeNumber");

      if (tvdbSeasonStr == null || tvdbEpisodeNumberStr == null) {
        return null;
      }

      Integer tvdbSeason = Integer.valueOf(tvdbSeasonStr);
      Integer tvdbEpisodeNumber = Integer.valueOf(tvdbEpisodeNumberStr);

      if (seasonNumber.equals(tvdbSeason) && episodeNumber.equals(tvdbEpisodeNumber)) {
        return fullObject;
      }
    }
    return null;
  }

}

