package com.mayhew3.gamesutil.games;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.games.Game;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class MetacriticGameUpdateRunner {

  private static SQLConnection connection;

  public static void main(String[] args) throws FileNotFoundException, SQLException, URISyntaxException {
    List<String> argList = Lists.newArrayList(args);
    Boolean allGames = argList.contains("AllGames");
    Boolean singleGame = argList.contains("SingleGame");
    Boolean logToFile = argList.contains("LogToFile");
    String identifier = new ArgumentChecker(args).getDBIdentifier();

    if (logToFile) {
      File file = new File("D:\\Projects\\mean_projects\\GamesDBUtil\\logs\\MetacriticGameUpdater.log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);
      System.setOut(ps);
    }

    connection = new PostgresConnectionFactory().createConnection(identifier);

    MetacriticGameUpdateRunner updateRunner = new MetacriticGameUpdateRunner();

    if (allGames) {
      updateRunner.updateAllGames();
    } else if (singleGame) {
      updateRunner.updateSingleGame();
    } else {
      updateRunner.updateUnmatchedGames();
    }
  }

  private void updateSingleGame() throws SQLException {
    String nameOfSingleGame = "DOOM";

    String sql = "SELECT * FROM games"
        + " WHERE title = ?";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, nameOfSingleGame);

    runUpdateOnResultSet(resultSet);
  }

  private void updateAllGames() throws SQLException {
    String sql = "SELECT * FROM games";
    ResultSet resultSet = connection.executeQuery(sql);

    runUpdateOnResultSet(resultSet);
  }

  private void updateUnmatchedGames() throws SQLException {
    String sql = "SELECT * FROM games"
     + " WHERE metacritic_matched IS NULL";
    ResultSet resultSet = connection.executeQuery(sql);

    runUpdateOnResultSet(resultSet);
  }

  private void runUpdateOnResultSet(ResultSet resultSet) throws SQLException {
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
