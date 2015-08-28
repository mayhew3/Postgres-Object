package com.mayhew3.gamesutil.games;

import com.mayhew3.gamesutil.mediaobject.Game;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MetacriticGameUpdateRunner {

  private static PostgresConnection connection;

  public static void main(String[] args) {
    connection = new PostgresConnection();

    MetacriticGameUpdateRunner updateRunner = new MetacriticGameUpdateRunner();
    updateRunner.runUpdate();
  }

  public void runUpdate() {
    updateShows();
  }

  private void updateShows() {
    ResultSet resultSet = connection.executeQuery("SELECT * FROM games");

    int i = 0;

    while (connection.hasMoreElements(resultSet)) {
      i++;

      Game game = new Game();
      try {
        game.initializeFromDBObject(resultSet);

        debug("Updating game: " + game.game.getValue());

        MetacriticGameUpdater metacriticGameUpdater = new MetacriticGameUpdater(game, connection);
        metacriticGameUpdater.runUpdater();
      } catch (GameFailedException e) {
        e.printStackTrace();
        debug("Show failed: " + game.game.getValue());
      } catch (SQLException e) {
        e.printStackTrace();
        debug("Failed to load game from database.");
      }

      debug(i + " processed.");
    }
  }


  protected static void debug(Object object) {
    System.out.println(object);
  }
}

