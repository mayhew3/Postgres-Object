package com.mayhew3.gamesutil;

import com.mongodb.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class FixEpisodesArrayUtility extends TVDatabaseUtility {


  public FixEpisodesArrayUtility() throws UnknownHostException {
    super("tv");

  }

  // declare mappings from old -> new column names
  // utility should copy the values over, and delete the old field.
  public static void main(String[] args) {
    try {
      FixEpisodesArrayUtility tiVoEpisodes = new FixEpisodesArrayUtility();
      tiVoEpisodes.upgradeRows();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

  }

  public void upgradeRows() {
    Boolean autoFix = false;

    DBCollection collection = _db.getCollection("series");
    DBCursor serieses = collection
        .find(new BasicDBObject()
                .append("episodes", new BasicDBObject("$exists", true))
        );

    Integer broken = 0;

    while(serieses.hasNext()) {

      DBObject series = serieses.next();



      List<Object> arrayNotCollection = new ArrayList<>();
      List<Object> collectionNotArray = new ArrayList<>();

      DBCollection episodesCollection = _db.getCollection("episodes");

      BasicDBList episodeArray = (BasicDBList) series.get("episodes");
      int episodesInArray = episodeArray.size();

      DBCursor cursor = episodesCollection.find(new BasicDBObject("SeriesId", series.get("_id")));
      List<Object> collectionIds = new ArrayList<>();

      while (cursor.hasNext()) {
        DBObject episode = cursor.next();

        Object episodeId = episode.get("_id");
        collectionIds.add(episodeId);

        if (!episodeArray.contains(episodeId)) {
          collectionNotArray.add(episodeId);
        }
      }

      for (Object episodeId : episodeArray) {
        if (!collectionIds.contains(episodeId)) {
          arrayNotCollection.add(episodeId);
        }
      }

      if (!arrayNotCollection.isEmpty() || !collectionNotArray.isEmpty()) {
        broken++;
        String errorMessage = "Series '" + series.get("SeriesTitle") + "' episodes array doesn't match episode ids found in collection " +
            "(" + episodesInArray + " array, " + collectionIds.size() + " coll): ";
        if (!arrayNotCollection.isEmpty()) {
          errorMessage += "Not in collection: {" + arrayNotCollection + "}. ";
        }
        if (!collectionNotArray.isEmpty()) {
          errorMessage += "Not in array: {" + collectionNotArray + "}. ";
        }
        debug(errorMessage);

        if (autoFix) {
          debug("Fixing.");
          BasicDBObject queryObject = new BasicDBObject("_id", series.get("_id"));

          if (!arrayNotCollection.isEmpty()) {
            BasicDBObject updateObject = new BasicDBObject("episodes", arrayNotCollection.toArray());
            collection.update(queryObject, new BasicDBObject("$pullAll", updateObject));
          }
          if (!collectionNotArray.isEmpty()) {
            BasicDBObject updateObject = new BasicDBObject("episodes", collectionNotArray.toArray());
            collection.update(queryObject, new BasicDBObject("$pushAll", updateObject));
          }

        /*
        for (Object episodeToRemove : arrayNotCollection) {
          BasicDBObject queryObject = new BasicDBObject("_id", series.get("_id"));
          BasicDBObject updateObject = new BasicDBObject("episodes", episodeToRemove);

          collection.update(queryObject, new BasicDBObject("$pull", updateObject));
        }
        for (Object episodeToAdd : collectionNotArray) {
          BasicDBObject queryObject = new BasicDBObject("_id", series.get("_id"));
          BasicDBObject updateObject = new BasicDBObject("episodes", episodeToAdd);

          collection.update(queryObject, new BasicDBObject("$addToSet", updateObject));
        }
        */
        }
      }




    }
    debug("Completed. " + broken + " shows had broken episode arrays.");
  }


}
