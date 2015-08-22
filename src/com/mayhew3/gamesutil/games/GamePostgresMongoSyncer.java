package com.mayhew3.gamesutil.games;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.sql.ResultSet;

public class GamePostgresMongoSyncer {

  public static void main(String[] args) {

    MongoConnection mongoConnection = new MongoConnection("games");

    DBCollection games = mongoConnection.getCollection("games");

    DBCursor dbObjects = games.find();

    while (dbObjects.hasNext()) {
      DBObject dbObject = dbObjects.next();
      Object game = dbObject.get("Game");
      debug((String) game);
    }

    debug("");
    debug("COMPLETE!");
    debug("");

    PostgresConnection connection = new PostgresConnection();

    ResultSet resultSet = connection.executeQuery("SELECT * FROM test_table");

    while (connection.hasMoreElements(resultSet)) {
      int id = connection.getInt(resultSet, "id");
      String name = connection.getString(resultSet, "name");

      debug(id + ", " + name);
    }
  }

  private static void debug(String debugString) {
    System.out.println(debugString);
  }

}
