package com.mayhew3.gamesutil.games;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mayhew3.gamesutil.DatabaseUtility;
import com.mayhew3.gamesutil.mediaobject.Game;
import com.mayhew3.gamesutil.mediaobject.GameLog;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class SteamGameUpdater extends DatabaseUtility {

  private static PostgresConnection connection;

  public static void main(String[] args) throws SQLException, FileNotFoundException {
    List<String> argList = Lists.newArrayList(args);
    Boolean logToFile = argList.contains("LogToFile");

    if (logToFile) {
      File file = new File("D:\\Projects\\mean_projects\\GamesDBUtil\\logs\\SteamUpdaterErrors.log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);

      System.err.println("Starting run on " + new Date());
    }

    connection = new PostgresConnection();
    updateFields();

    debug(" --- ");
    debug(" Finished Steam API section, starting attribute update!");
    debug(" --- ");

    SteamAttributeUpdateRunner steamAttributeUpdateRunner = new SteamAttributeUpdateRunner(connection);
    steamAttributeUpdateRunner.runSteamAttributeUpdate();

    debug(" --- ");
    debug(" Finished Steam Attribute section, starting HowLongToBeat update!");
    debug(" --- ");

    HowLongToBeatUpdateRunner howLongToBeatUpdateRunner = new HowLongToBeatUpdateRunner(connection);
    howLongToBeatUpdateRunner.runUpdate();

    debug(" --- ");
    debug(" Full operation complete!");
  }

  public static void updateFields() throws SQLException {
    Map<Integer, String> unfoundGames = new HashMap<Integer, String>();
    ArrayList<String> duplicateGames = new ArrayList<String>();

    String fullURL = getFullUrl();

    Set<Integer> jsonSteamIDs = Sets.newHashSet();

    try {
      JSONObject jsonObject = readJsonFromUrl(fullURL);
      JSONArray jsonArray = jsonObject.getJSONObject("response").getJSONArray("games");

      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject jsonGame = jsonArray.getJSONObject(i);

        processSteamGame(unfoundGames, duplicateGames, jsonGame);

        jsonSteamIDs.add(jsonGame.getInt("appid"));
      }

      debug("Operation finished!");
//      debug(jsonObject);
    } catch (IOException e) {
      debug("Error reading from URL: " + fullURL);
      e.printStackTrace();
    }

    debug("");
    debug("Updating ownership of games no longer in steam library...");
    debug("");

    ResultSet resultSet = connection.executeQuery("SELECT * FROM games WHERE steamid is not null AND owned = 'owned'");

    while (connection.hasMoreElements(resultSet)) {
      Integer steamid = connection.getInt(resultSet, "steamid");

      if (!jsonSteamIDs.contains(steamid)) {
        debug(connection.getString(resultSet, "title") + ": no longer found!");

        Game game = new Game();
        game.initializeFromDBObject(resultSet);
        game.owned.changeValue("not owned");
        game.commit(connection);
      }
    }

  }

  private static void processSteamGame(Map<Integer, String> unfoundGames, ArrayList<String> duplicateGames, JSONObject jsonGame) throws SQLException {
    String name = jsonGame.getString("name");
    Integer steamID = jsonGame.getInt("appid");
    BigDecimal playtime = new BigDecimal(jsonGame.getInt("playtime_forever"));
    String icon = jsonGame.getString("img_icon_url");
    String logo = jsonGame.getString("img_logo_url");

    debug(name + ": looking for updates.");

    // if changed (match on appid, not name):
    // - update playtime
    // - update "Steam Name" column
    // - add log that it changed? How to know if it just differs from what I have,
    //    of if I've actually played since that was there? Confirmed flag? Or
    //    log is just a stamp of the date of checking and the total time?
    //    LOG: Previous Hours, Current Hours, Change. For first time, Prev and Curr are the same, and Change is 0.

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch("SELECT * FROM games WHERE steamid = ?", steamID);

    Game game = new Game();
    if (connection.hasMoreElements(resultSet)) {
      game.initializeFromDBObject(resultSet);
      updateGame(name, steamID, playtime, icon, logo, game);
    } else {
      debug(" - Game not found! Adding.");
      addNewGame(name, steamID, playtime, icon, logo, game);
      unfoundGames.put(steamID, name);
    }

    if (connection.hasMoreElements(resultSet)) {
      duplicateGames.add(name + "(" + steamID + ")");
    }
  }

  private static void updateGame(String name, Integer steamID, BigDecimal playtime, String icon, String logo, Game game) {
    game.logo.changeValue(logo);
    game.icon.changeValue(icon);
    game.title.changeValue(name);
    game.owned.changeValue("owned");

    BigDecimal previousPlaytime = game.playtime.getValue();
    if (!(playtime.compareTo(previousPlaytime) == 0)) {
      if (previousPlaytime != null) {
        logUpdateToPlaytime(name, steamID, previousPlaytime, playtime);
      }
      game.playtime.changeValue(playtime);
    }
    game.commit(connection);
  }

  private static void addNewGame(String name, Integer steamID, BigDecimal playtime, String icon, String logo, Game game) {
    if (playtime.compareTo(BigDecimal.ZERO) > 0) {
      logUpdateToPlaytime(name, steamID, BigDecimal.ZERO, playtime);
    }

    game.initializeForInsert();

    game.platform.changeValue("Steam");
    game.owned.changeValue("owned");
    game.started.changeValue(false);
    game.added.changeValue(new Timestamp(new Date().getTime()));
    game.title.changeValue(name);
    game.steamID.changeValue(steamID);
    game.playtime.changeValue(playtime);
    game.icon.changeValue(icon);
    game.logo.changeValue(logo);
    game.metacriticPage.changeValue(false);

    game.commit(connection);
  }

  private static void logUpdateToPlaytime(String name, Integer steamID, BigDecimal previousPlaytime, BigDecimal updatedPlaytime) {
    GameLog gameLog = new GameLog();
    gameLog.initializeForInsert();

    gameLog.game.changeValue(name);
    gameLog.steamID.changeValue(steamID);
    gameLog.platform.changeValue("Steam");
    gameLog.previousPlaytime.changeValue(previousPlaytime);
    gameLog.updatedplaytime.changeValue(updatedPlaytime);
    gameLog.diff.changeValue(updatedPlaytime.subtract(previousPlaytime));
    gameLog.eventtype.changeValue("Played");
    gameLog.eventdate.changeValue(new Timestamp(new Date().getTime()));

    gameLog.commit(connection);
  }


}
