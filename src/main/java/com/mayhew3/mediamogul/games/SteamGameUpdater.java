package com.mayhew3.mediamogul.games;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mayhew3.mediamogul.ArgumentChecker;
import com.mayhew3.mediamogul.DatabaseUtility;
import com.mayhew3.mediamogul.scheduler.UpdateRunner;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.model.games.Game;
import com.mayhew3.mediamogul.model.games.GameLog;
import com.mayhew3.mediamogul.tv.helper.UpdateMode;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class SteamGameUpdater extends DatabaseUtility implements UpdateRunner {

  private SQLConnection connection;

  public SteamGameUpdater(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String... args) throws SQLException, FileNotFoundException, URISyntaxException, InterruptedException {
    List<String> argList = Lists.newArrayList(args);
    Boolean logToFile = argList.contains("LogToFile");
    ArgumentChecker argumentChecker = new ArgumentChecker(args);

    if (logToFile) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
      String dateFormatted = simpleDateFormat.format(new Date());

      String mediaMogulLogs = System.getenv("MediaMogulLogs");

      File errorFile = new File(mediaMogulLogs + "\\SteamUpdaterErrors_" + dateFormatted + "_" + argumentChecker.getDBIdentifier() + ".log");
      FileOutputStream errorStream = new FileOutputStream(errorFile, true);
      PrintStream ps = new PrintStream(errorStream);
      System.setErr(ps);

      System.err.println("Starting run on " + new Date());

      File logFile = new File(mediaMogulLogs + "\\SteamUpdaterLog_" + dateFormatted + "_" + argumentChecker.getDBIdentifier() + ".log");
      FileOutputStream logStream = new FileOutputStream(logFile, true);
      PrintStream logPrintStream = new PrintStream(logStream);
      System.setOut(logPrintStream);
    }

    debug("");
    debug("SESSION START! Date: " + new Date());
    debug("");

    SQLConnection connection = PostgresConnectionFactory.createConnection(argumentChecker);
    SteamGameUpdater steamGameUpdater = new SteamGameUpdater(connection);
    steamGameUpdater.runUpdate();

    debug(" --- ");
    debug(" Full operation complete!");
  }

  public void runUpdate() throws SQLException {
    Map<Integer, String> unfoundGames = new HashMap<>();
    ArrayList<String> duplicateGames = new ArrayList<>();

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
    } catch (IOException e) {
      debug("Error reading from URL: " + fullURL);
      e.printStackTrace();
    }

    debug("");
    debug("Updating ownership of games no longer in steam library...");
    debug("");

    ResultSet resultSet = connection.executeQuery("SELECT * FROM game WHERE steamid is not null AND owned = 'owned'");

    while (resultSet.next()) {
      Integer steamid = resultSet.getInt("steamid");

      if (!jsonSteamIDs.contains(steamid)) {
        debug(resultSet.getString("title") + ": no longer found!");

        Game game = new Game();
        game.initializeFromDBObject(resultSet);
        game.owned.changeValue("not owned");
        game.commit(connection);
      }
    }

  }

  private void processSteamGame(Map<Integer, String> unfoundGames, ArrayList<String> duplicateGames, JSONObject jsonGame) throws SQLException {
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

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch("SELECT * FROM game WHERE steamid = ?", steamID);

    Game game = new Game();
    if (resultSet.next()) {
      game.initializeFromDBObject(resultSet);
      updateGame(name, steamID, playtime, icon, logo, game);
    } else {
      debug(" - Game not found! Adding.");
      addNewGame(name, steamID, playtime, icon, logo, game);
      unfoundGames.put(steamID, name);
    }

    if (resultSet.next()) {
      duplicateGames.add(name + "(" + steamID + ")");
    }
  }

  private void updateGame(String name, Integer steamID, BigDecimal playtime, String icon, String logo, Game game) throws SQLException {
    game.logo.changeValue(logo);
    game.icon.changeValue(icon);
    game.title.changeValue(name);
    game.owned.changeValue("owned");

    BigDecimal previousPlaytime = game.playtime.getValue() == null ? BigDecimal.ZERO : game.playtime.getValue();
    if (!(playtime.compareTo(previousPlaytime) == 0)) {
      logUpdateToPlaytime(name, steamID, previousPlaytime, playtime, game.id.getValue());
      game.playtime.changeValue(playtime);
      game.lastPlayed.changeValue(new Timestamp(bumpDateIfLateNight().toDate().getTime()));
    }
    game.commit(connection);
  }

  private void addNewGame(String name, Integer steamID, BigDecimal playtime, String icon, String logo, Game game) throws SQLException {
    game.initializeForInsert();

    Boolean needsPlaytimeUpdate = playtime.compareTo(BigDecimal.ZERO) > 0;

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

    if (needsPlaytimeUpdate) {
      logUpdateToPlaytime(name, steamID, BigDecimal.ZERO, playtime, game.id.getValue());
      game.lastPlayed.changeValue(new Timestamp(bumpDateIfLateNight().toDate().getTime()));
      game.commit(connection);
    }
  }

  private void logUpdateToPlaytime(String name, Integer steamID, BigDecimal previousPlaytime, BigDecimal updatedPlaytime, Integer gameID) throws SQLException {
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
    gameLog.gameID.changeValue(gameID);

    gameLog.commit(connection);
  }


  /**
   * Most of the time this updater runs at 4:45am, so if this method is called between 12am and 7am, and new playtime is
   * found, assume for now that it was played the previous day. Arbitrarily put 8pm.
   * @return The current timestamp if it is after 7am, or 8pm the previous day otherwise.
   */
  private static DateTime bumpDateIfLateNight() {
    DateTime today = new DateTime(new Date());
    if (today.getHourOfDay() > 7) {
      return today;
    } else {
      return today.minusDays(1).withHourOfDay(20);
    }
  }

  @Override
  public String getRunnerName() {
    return "Steam Game Updater";
  }

  @Override
  public @Nullable UpdateMode getUpdateMode() {
    return null;
  }
}
