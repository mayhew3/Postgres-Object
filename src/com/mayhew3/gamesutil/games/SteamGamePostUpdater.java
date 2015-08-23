package com.mayhew3.gamesutil.games;

import com.mayhew3.gamesutil.DatabaseUtility;
import com.mayhew3.gamesutil.mediaobjectpostgres.Game;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SteamGamePostUpdater extends DatabaseUtility {

  public static void main(String[] args) throws SQLException {
    PostgresConnection connection = new PostgresConnection();
    updateFields(connection);

  }

  public static void updateFields(PostgresConnection connection) throws SQLException {
    Map<Integer, String> unfoundGames = new HashMap<Integer, String>();
    ArrayList<String> duplicateGames = new ArrayList<String>();

    String fullURL = getFullUrl();


    try {
      JSONObject jsonObject = readJsonFromUrl(fullURL);
      JSONArray jsonArray = jsonObject.getJSONObject("response").getJSONArray("games");

      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject jsonGame = jsonArray.getJSONObject(i);
        SteamGame steamGame = new SteamGame(jsonGame, _db);

        debug(steamGame.getName() + ": looking for updates.");

        // if changed (match on appid, not name):
        // - update playtime
        // - update "Steam Name" column
        // - add log that it changed? How to know if it just differs from what I have,
        //    of if I've actually played since that was there? Confirmed flag? Or
        //    log is just a stamp of the date of checking and the total time?
        //    LOG: Previous Hours, Current Hours, Change. For first time, Prev and Curr are the same, and Change is 0.

        ResultSet resultSet = connection.prepareAndExecuteStatementFetch("SELECT * FROM games WHERE steamid = ?", steamGame.getID());

        Game game = new Game();
        if (connection.hasMoreElements(resultSet)) {
          game.initializeFromDBObject(resultSet);
          steamGame.updateFieldsOnGameObject(game);
        } else {
          debug(" - Game not found! Adding.");
          game.initializeForInsert();
          steamGame.copyFieldsToGameObject(game);
          game.platform.changeValue("Steam");
          game.owned.changeValue("true");
          game.started.changeValue(false);
          game.added.changeValue(new Date());
          unfoundGames.put(steamGame.getID(), steamGame.getName());
        }

        if (connection.hasMoreElements(resultSet)) {
          duplicateGames.add(steamGame.getName() + "(" + steamGame.getID() + ")");
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
