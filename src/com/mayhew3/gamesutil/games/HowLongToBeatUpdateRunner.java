package com.mayhew3.gamesutil.games;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.mediaobject.Game;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class HowLongToBeatUpdateRunner {

  private PostgresConnection connection;

  public HowLongToBeatUpdateRunner(PostgresConnection connection) {
    this.connection = connection;
  }

  public static void main(String[] args) throws FileNotFoundException {
    List<String> argList = Lists.newArrayList(args);
    Boolean logToFile = argList.contains("LogToFile");

    if (logToFile) {
      File file = new File("D:\\Projects\\mean_projects\\GamesDBUtil\\logs\\HowLongToBeatUpdater.log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);
      System.setOut(ps);
    }

    PostgresConnection connection = new PostgresConnection();

    HowLongToBeatUpdateRunner updateRunner = new HowLongToBeatUpdateRunner(connection);

    updateRunner.runUpdate();

  }

  public void runUpdate() {
    String sql = "SELECT * FROM games WHERE howlong_id IS NOT NULL AND howlong_updated IS NULL AND owned <> 'not owned'";
//    String sql = "SELECT * FROM games WHERE title = 'ICO'";
    ResultSet resultSet = connection.executeQuery(sql);

    int i = 0;

    ChromeDriver chromeDriver = new ChromeDriver();

    while (connection.hasMoreElements(resultSet)) {
      i++;

      Game game = new Game();
      try {
        game.initializeFromDBObject(resultSet);

        debug("Updating game: " + game);

        HowLongToBeatUpdater updater = new HowLongToBeatUpdater(game, connection, chromeDriver);
        updater.runUpdater();
      } catch (SQLException e) {
        e.printStackTrace();
        debug("Game failed to load from DB.");
      } catch (GameFailedException e) {
        e.printStackTrace();
        debug("Game failed: " + game);
        logFailure(game);
      } catch (WebDriverException e) {
        e.printStackTrace();
        debug("WebDriver error: " + game);
        logFailure(game);
      }

      debug(i + " processed.");
    }

    chromeDriver.close();
  }

  private void logFailure(Game game) {
    game.howlong_failed.changeValue(new Timestamp(new Date().getTime()));
    game.commit(connection);
  }


  protected void debug(Object object) {
    System.out.println(object);
  }
}

