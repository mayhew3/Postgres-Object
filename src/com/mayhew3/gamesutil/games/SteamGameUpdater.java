package com.mayhew3.gamesutil.games;

import com.mayhew3.gamesutil.DatabaseUtility;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SteamGameUpdater extends DatabaseUtility {

  public static void main(String[] args) {
    try {
      connect("games");
      logActivity("StartUpdate");
      updateFields();
      logActivity("EndUpdate");
      closeDatabase();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

  }

  private static void logActivity(String startUpdate) {
    DBCollection collection = _db.getCollection("connectlogs");
    BasicDBObject basicDBObject = new BasicDBObject("Type", startUpdate)
        .append("LogTime", new Date());
    try {
      collection.insert(basicDBObject);
    } catch (MongoException e) {
      throw new RuntimeException("Error inserting log into database.\r\n" + e.getLocalizedMessage());
    }
  }

  public static void updateFields() {
    Map<Integer, String> unfoundGames = new HashMap<Integer, String>();
    ArrayList<String> duplicateGames = new ArrayList<String>();

    String fullURL = getFullUrl();


    try {
      JSONObject jsonObject = readJsonFromUrl(fullURL);
      JSONArray jsonArray = jsonObject.getJSONObject("response").getJSONArray("games");

      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject game = jsonArray.getJSONObject(i);
        SteamGame steamGame = new SteamGame(game, _db);

        // if changed (match on appid, not name):
        // - update playtime
        // - update "Steam Name" column
        // - add log that it changed? How to know if it just differs from what I have,
        //    of if I've actually played since that was there? Confirmed flag? Or
        //    log is just a stamp of the date of checking and the total time?
        //    LOG: Previous Hours, Current Hours, Change. For first time, Prev and Curr are the same, and Change is 0.

        BasicDBObject query = new BasicDBObject("SteamID", steamGame.getID());

        DBCursor cursor = _collection.find(query);

        if (cursor.count() == 1) {
          steamGame.updateDatabase(cursor.next());
        } else if (cursor.count() > 1) {
          duplicateGames.add(steamGame.getName() + "(" + steamGame.getID() + ")");
        } else {
          steamGame.addToDatabase();
          unfoundGames.put(steamGame.getID(), steamGame.getName());
        }
      }

      debug("Operation finished!");
//      debug(jsonObject);
    } catch (IOException e) {
      debug("Error reading from URL: " + fullURL);
      e.printStackTrace();
    }

  }


}
