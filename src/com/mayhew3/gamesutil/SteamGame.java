package com.mayhew3.gamesutil;

import com.mongodb.*;
import org.json.JSONObject;

import java.util.Date;

public class SteamGame {
  private String name;
  private Integer steamID;
  private Integer playtime;
  private String icon;
  private String logo;

  public SteamGame(JSONObject game) {
    name = game.getString("name");
    steamID = game.getInt("appid");
    playtime = game.getInt("playtime_forever");
    icon = game.getString("img_icon_url");
    logo = game.getString("img_logo_url");
  }

  public String getName() {
    return name;
  }

  public Integer getID() {
    return steamID;
  }

  public void updateDatabase(DBCollection collection, DBObject gameDocument) {
    Integer previousPlaytime = (Integer)(gameDocument.get("Playtime"));
    if (!playtime.equals(previousPlaytime)) {
      if (previousPlaytime != null) {
        DB db = collection.getDB();
        DBCollection gamelogs = db.getCollection("gamelogs");
        logUpdateToPlaytime(gamelogs, previousPlaytime);
      }
      updatePlaytime(collection);
    }
  }


  public void logUpdateToPlaytime(DBCollection collection, Integer previousPlaytime) {
    try {

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


  private void updatePlaytime(DBCollection collection) {
    BasicDBObject query = new BasicDBObject("SteamID", steamID);
    BasicDBObject updateObject = new BasicDBObject();
    updateObject.append("$set", new BasicDBObject().append("Playtime", playtime));

    collection.update(query, updateObject);
  }


  public void addToDatabase(DBCollection collection) {
    insertGameToCollection(collection);
    if (playtime > 0) {
      logUpdateToPlaytime(collection, 0);
    }
  }

  private void insertGameToCollection(DBCollection collection) {
    try {
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
