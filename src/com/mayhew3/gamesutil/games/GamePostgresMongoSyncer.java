package com.mayhew3.gamesutil.games;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class GamePostgresMongoSyncer {

  private static MongoConnection mongoConnection;
  private static PostgresConnection postgresConnection;

  public static void main(String[] args) {

    mongoConnection = new MongoConnection("games");

    postgresConnection = new PostgresConnection();

    eraseAndCopyGames();

    eraseAndCopyGamelogs();

  }

  private static void eraseAndCopyGames() {
    postgresConnection.executeUpdate("TRUNCATE TABLE games");
    postgresConnection.executeUpdate("ALTER SEQUENCE games_id_seq RESTART WITH 1");

    DBCollection games = mongoConnection.getCollection("games");

    DBCursor dbObjects = games.find();

    while (dbObjects.hasNext()) {
      DBObject dbObject = dbObjects.next();
      Object game = dbObject.get("Game");
      debug("Processing game: " + game);
      translateRow(dbObject, postgresConnection, "games");
    }


    debug("");
    debug("COMPLETE!");
    debug("");

  }

  private static void eraseAndCopyGamelogs() {
    postgresConnection.executeUpdate("TRUNCATE TABLE gamelogs");
    postgresConnection.executeUpdate("ALTER SEQUENCE gamelogs_id_seq RESTART WITH 1");

    DBCollection gamelogs = mongoConnection.getCollection("gamelogs");

    DBCursor dbObjects = gamelogs.find();

    while (dbObjects.hasNext()) {
      DBObject dbObject = dbObjects.next();
      Object game = dbObject.get("Game");
      Object eventDate = dbObject.get("EventDate");
      debug("Processing game log: " + game + ": " + eventDate);
      translateRow(dbObject, postgresConnection, "gamelogs");
    }


    debug("");
    debug("COMPLETE!");
    debug("");

  }

  private static void debug(String debugString) {
    System.out.println(debugString);
  }

  private static void translateRow(DBObject dbObject, PostgresConnection connection, String tableName) {
    List<String> fieldNames = Lists.newArrayList();
    List<Object> fieldValues = Lists.newArrayList();
    List<String> questionMarks = Lists.newArrayList();

    Joiner joiner = Joiner.on(", ");


    Set<String> keySet = dbObject.keySet();
    keySet.remove("_id");

    for (String fieldName : keySet) {
      fieldNames.add("\"" + fieldName.toLowerCase() + "\"");
      questionMarks.add("?");

      Object value = dbObject.get(fieldName);
      if ("Percent Done".equalsIgnoreCase(fieldName)) {
        String replaced = ((String) value).replace("%", "");
        fieldValues.add(Integer.valueOf(replaced));
      } else if ("Include".equalsIgnoreCase(fieldName) && value instanceof Integer) {
        Integer integerValue = (Integer) value;
        Boolean boolValue = (integerValue == 1);
        fieldValues.add(boolValue);
      } else if (value instanceof Date) {
        Date dateValue = (Date) value;
        Timestamp timestamp = new Timestamp(dateValue.getTime());
        fieldValues.add(timestamp);
      } else if ("Owned".equalsIgnoreCase(fieldName) || value instanceof String) {
        if ("Added".equalsIgnoreCase(fieldName) || "Finished".equalsIgnoreCase(fieldName)) {
          String stringValue = (String)value;
          Timestamp timestamp = new Timestamp(Date.parse(stringValue));
          fieldValues.add(timestamp);
        } else if ("Started".equalsIgnoreCase(fieldName)) {
          Boolean boolValue = "x".equals(value);
          fieldValues.add(boolValue);
        } else {
          fieldValues.add(String.valueOf(value));
        }
      } else {
        fieldValues.add(value);
      }
    }

    String commaSeparatedNames = joiner.join(fieldNames);
    String commaSeparatedQuestionMarks = joiner.join(questionMarks);

    String sql = "INSERT INTO " + tableName + " (" + commaSeparatedNames + ") VALUES (" + commaSeparatedQuestionMarks + ")";
    connection.prepareAndExecuteStatementUpdate(sql, fieldValues);


  }

}
