package com.mayhew3.gamesutil.games;

import com.mayhew3.gamesutil.DatabaseUtility;
import com.mongodb.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.mayhew3.gamesutil.InputGetter.*;

public class FirstTimeGameMatcher extends DatabaseUtility {

  public static void main(String[] args) {
    try {
      connect("games");
      matchSteamIDs();
      closeDatabase();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

  }

  public static void matchSteamIDs() {
    Map<Integer, String> unfoundGames = new HashMap<Integer, String>();
    ArrayList<String> duplicateGames = new ArrayList<String>();
    ArrayList<String> alreadyProcessed = new ArrayList<String>();

    String fullURL = getFullUrl();


    try {
      JSONObject jsonObject = readJsonFromUrl(fullURL);
      JSONArray jsonArray = jsonObject.getJSONObject("response").getJSONArray("games");

      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject game = jsonArray.getJSONObject(i);
        String gameName = game.getString("name");
        Integer gameSteamID = game.getInt("appid");

        if (isAlreadyProcessed(gameSteamID)) {
          alreadyProcessed.add(gameName);
        } else {

          BasicDBObject query = new BasicDBObject("Game", gameName)
              .append("Platform", "Steam")
              .append("SteamID", new BasicDBObject("$exists", false));

          DBCursor cursor = _collection.find(query);

          if (cursor.count() == 1) {
            updateGame(gameSteamID, query);
          } else if (!cursor.hasNext()) {
            if (!findAlternateName(gameName, gameSteamID)) {
              unfoundGames.put(gameSteamID, gameName);
            }
          } else {
            duplicateGames.add(gameName);
          }
        }

      }

      if (!unfoundGames.isEmpty()) {
        debug("Unfound games: " + unfoundGames.size());
        for (Integer unfoundID : unfoundGames.keySet()) {
          debug(unfoundGames.get(unfoundID) + " (" + unfoundID + ")");
        }
        Integer addGames = new Integer(grabInput("Add all unfound games to database? (0 or 1) "));
        if (addGames > 0) {
          for (Integer steamID : unfoundGames.keySet()) {
            addGame(steamID, unfoundGames.get(steamID));
          }
        }
      }
      if (!duplicateGames.isEmpty()) {
        debug("Duplicate games: " + duplicateGames);
      }
      debug("Operation finished!");
//      debug(jsonObject);
    } catch (IOException e) {
      debug("Error reading from URL: " + fullURL);
      e.printStackTrace();
    }

  }

  private static boolean isAlreadyProcessed(int steamID) {
    BasicDBObject query = new BasicDBObject("SteamID", steamID);
    return _collection.find(query).hasNext();
  }

  private static boolean findAlternateName(String steamName, int steamID) {
    for (int i = 3; i < steamName.length(); i++) {
      String substring = steamName.substring(0, i);
      // Starts with at least first three characters
      // todo: find library that has better string matching. load all names from JSON and DB into memory,
      // todo: and find best match
      Pattern pattern = Pattern.compile("^" + Pattern.quote(substring) + ".*", Pattern.CASE_INSENSITIVE);
      BasicDBObject query = new BasicDBObject("Game", pattern)
          .append("Platform", "Steam")
          .append("SteamID", new BasicDBObject("$exists", false));

      DBCursor cursor = _collection.find(query);

      if (cursor.count() == 1) {
        String foundName = (String) cursor.next().get("Game");
        Integer inputResult = new Integer(grabInput("Steam name: '" + steamName + "'. Found name: '" + foundName + "'. Match? (0 or 1) "));
        if (inputResult > 0) {
          updateGame(steamID, query);
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  }

  private static void updateGame(Integer gameSteamID, BasicDBObject query) {
    BasicDBObject updateObject = new BasicDBObject();
    updateObject.append("$set", new BasicDBObject().append("SteamID", gameSteamID));

    _collection.update(query, updateObject);
  }


}
