package com.mayhew3.gamesutil;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.net.UnknownHostException;
import java.util.Date;

public class UpdateMatchStumps extends TVDatabaseUtility {


  public UpdateMatchStumps() throws UnknownHostException {
    super("tv");

  }

  // declare mappings from old -> new column names
  // utility should copy the values over, and delete the old field.
  public static void main(String[] args) {
    try {
      UpdateMatchStumps matchStumps = new UpdateMatchStumps();
      matchStumps.upgradeRows();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

  }

  public void upgradeRows() {
    DBCursor cursor = _db.getCollection("episodes").find(
        new BasicDBObject("tvdbEpisodeId", new BasicDBObject("$exists", true))
    );

    if (cursor.hasNext()) {
      DBObject next = cursor.next();

      Episode episode = new Episode(next);
      Date showingStartTime = episode.tiVoShowingStartTime.getValue();
      Boolean watched = episode.tiVoSuggestion.getValue();
      Date dateAdded = episode.dateAdded.getValue();

      Date firstAired = episode.tvdbFirstAired.getValue();

      String episodeTitle = episode.tivoEpisodeTitle.getValue();

      int poop = 0;
    }
  }


}
