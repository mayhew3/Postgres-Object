package com.mayhew3.gamesutil.games;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.db.PostgresConnection;
import com.mayhew3.gamesutil.mediaobject.Game;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class MetacriticGameUpdateRunner {

  private static PostgresConnection connection;

  public static void main(String[] args) throws FileNotFoundException, SQLException {
    List<String> argList = Lists.newArrayList(args);
    Boolean allGames = argList.contains("AllGames");
    Boolean logToFile = argList.contains("LogToFile");

    if (logToFile) {
      File file = new File("D:\\Projects\\mean_projects\\GamesDBUtil\\logs\\MetacriticGameUpdater.log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);
      System.setOut(ps);
    }

    connection = new PostgresConnection();

    MetacriticGameUpdateRunner updateRunner = new MetacriticGameUpdateRunner();
    updateRunner.runUpdate(allGames);
  }

  public void runUpdate(Boolean allGames) throws SQLException {
    updateGames(allGames);
  }

  private void updateGames(Boolean allGames) throws SQLException {
    String sql = "SELECT * FROM games";
    if (!allGames) {
      sql += " WHERE metacritic_matched IS NULL";
    }
    ResultSet resultSet = connection.executeQuery(sql);

    int i = 1;
    int failures = 0;

    while (resultSet.next()) {
      Game game = new Game();
      try {
        game.initializeFromDBObject(resultSet);

        debug("Updating game: " + game.title.getValue());

        MetacriticGameUpdater metacriticGameUpdater = new MetacriticGameUpdater(game, connection);
        metacriticGameUpdater.runUpdater();
      } catch (GameFailedException e) {
        e.printStackTrace();
        debug("Show failed: " + game.title.getValue());
        failures++;
      } catch (SQLException e) {
        e.printStackTrace();
        debug("Failed to load game from database.");
        failures++;
      }

      debug(i + " processed.");
      i++;
    }

    if (i > 1) {
      debug("Operation completed! Failed on " + failures + "/" + (i - 1) + " games (" + (100 * failures / (i - 1)) + "%)");
    }
  }


  protected static void debug(Object object) {
    System.out.println(new Date() + ": " + object);
  }
}

