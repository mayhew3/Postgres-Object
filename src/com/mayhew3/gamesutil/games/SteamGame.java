package com.mayhew3.gamesutil.games;

import com.mayhew3.gamesutil.mediaobjectpostgres.Game;
import com.mayhew3.gamesutil.mediaobjectpostgres.GameLog;
import com.mongodb.*;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

public class SteamGame {
  private String name;
  private Integer steamID;
  private Integer playtime;
  private String icon;
  private String logo;

  private DB db;

  public SteamGame(JSONObject game, DB db) {
    name = game.getString("name");
    steamID = game.getInt("appid");
    playtime = game.getInt("playtime_forever");
    icon = game.getString("img_icon_url");
    logo = game.getString("img_logo_url");
    this.db = db;
  }

  public String getName() {
    return name;
  }

  public Integer getID() {
    return steamID;
  }

  public void updateDatabase(DBObject gameDocument) {
    Integer previousPlaytime = (Integer)(gameDocument.get("Playtime"));
    if (!playtime.equals(previousPlaytime)) {
      if (previousPlaytime != null) {
        logUpdateToPlaytime(previousPlaytime);
      }
      updatePlaytime();
    }
  }

  public void updateFieldsOnGameObject(Game game, PostgresConnection connection) {
    BigDecimal previousPlaytime = game.playtime.getValue();
    if (!(new BigDecimal(playtime)).equals(previousPlaytime)) {
      debug(" - Updating new play time!");
      if (previousPlaytime != null) {
        logUpdateToPlaytimePostgres(previousPlaytime, connection);
      }
      game.playtime.changeValue(new BigDecimal(playtime));
      game.commit(connection);
    } else {
      debug(" - playtime unchanged.");
    }
  }

  public void copyFieldsToGameObjectAndInsert(Game game, PostgresConnection connection) {
    if (playtime > 0) {
      logUpdateToPlaytimePostgres(BigDecimal.ZERO, connection);
    }
    game.platform.changeValue("Steam");
    game.owned.changeValue("true");
    game.started.changeValue(false);
    game.added.changeValue(new Timestamp(new Date().getTime()));
    game.game.changeValue(name);
    game.steamID.changeValue(steamID);
    game.playtime.changeValue(new BigDecimal(playtime));
    game.icon.changeValue(icon);
    game.logo.changeValue(logo);

    game.commit(connection);
  }

  public void logUpdateToPlaytimePostgres(BigDecimal previousPlaytime, PostgresConnection connection) {
    int previousInteger = previousPlaytime.intValue();
    int diff = playtime - previousInteger;

    GameLog gameLog = new GameLog();
    gameLog.initializeForInsert();

    gameLog.game.changeValue(name);
    gameLog.steamID.changeValue(steamID);
    gameLog.platform.changeValue("Steam");
    gameLog.previousPlaytime.changeValue(previousInteger);
    gameLog.updatedplaytime.changeValue(playtime);
    gameLog.diff.changeValue(diff);
    gameLog.eventtype.changeValue("Played");
    gameLog.eventdate.changeValue(new Timestamp(new Date().getTime()));

    gameLog.commit(connection);
  }

  public void logUpdateToPlaytime(Integer previousPlaytime) {
    try {
      DBCollection collection = db.getCollection("gamelogs");
      // todo: figure out FK (Object ref) from log table to game  (not a thing, i think)
      debug("Played game '" + name + "' for " + (playtime - previousPlaytime) + " minutes.");
      BasicDBObject gameObject = new BasicDBObject("Game", name)
          .append("SteamID", steamID)
          .append("Platform", "Steam")
          .append("EventType", "Played")
          .append("EventDate", new Date())
          .append("PreviousPlaytime", previousPlaytime)
          .append("UpdatedPlaytime", playtime)
          .append("Diff", playtime - previousPlaytime)
          ;
      collection.insert(gameObject);
    } catch (MongoException e) {
      debug("Error inserting game '" + name + "' (" + steamID + "). Exception: ");
      e.printStackTrace();
    }
  }


  private void updatePlaytime() {
    DBCollection collection = db.getCollection("games");

    BasicDBObject query = new BasicDBObject("SteamID", steamID);
    BasicDBObject updateObject = new BasicDBObject();
    updateObject.append("$set", new BasicDBObject().append("Playtime", playtime));

    collection.update(query, updateObject);
  }


  public void addToDatabase() {
    insertGameToCollection();
    if (playtime > 0) {
      logUpdateToPlaytime(0);
    }
  }

  private void insertGameToCollection() {
    try {
      DBCollection collection = db.getCollection("games");

      debug("Adding new game: '" + name + "'");
      BasicDBObject gameObject = new BasicDBObject("Game", name)
          .append("SteamID", steamID)
          .append("Platform", "Steam")
          .append("Owned", true)
          .append("Started", false)
          .append("Added", new Date())
          .append("Icon", icon)
          .append("Logo", logo)
          .append("Playtime", playtime);
      collection.insert(gameObject);
    } catch (MongoException e) {
      debug("Error inserting game '" + name + "' (" + steamID + "). Exception: ");
      e.printStackTrace();
    }
  }


  protected static void debug(Object object) {
    System.out.println(object);
  }

}
