package com.mayhew3.gamesutil.games;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.mediaobject.Game;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SteamAttributeUpdateRunner {

  private PostgresConnection connection;

  public SteamAttributeUpdateRunner(PostgresConnection connection) {
    this.connection = connection;
  }

  public static void main(String[] args) throws FileNotFoundException {
    List<String> argList = Lists.newArrayList(args);
    Boolean logToFile = argList.contains("LogToFile");
    Boolean redo = false;

    if (logToFile) {
      File file = new File("D:\\Projects\\mean_projects\\GamesDBUtil\\logs\\SteamAttributeUpdater.log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);
      System.setOut(ps);
    }

    PostgresConnection connection = new PostgresConnection();

    if (redo) {
      connection.executeUpdate("TRUNCATE TABLE steam_attributes");
      connection.executeUpdate("ALTER SEQUENCE steam_attributes_id_seq RESTART WITH 1");
      connection.executeUpdate("UPDATE games SET steam_attributes = NULL");
    }

    SteamAttributeUpdateRunner updateRunner = new SteamAttributeUpdateRunner(connection);

    updateRunner.runSteamAttributeUpdate();

  }

  public void runSteamAttributeUpdate() {
    ChromeDriver chromeDriver = new ChromeDriver();

    String sql = "SELECT * FROM games WHERE steamid is not null AND steam_attributes IS NULL";
    ResultSet resultSet = connection.executeQuery(sql);

    int i = 0;

    while (connection.hasMoreElements(resultSet)) {
      i++;

      Game game = new Game();
      try {
        game.initializeFromDBObject(resultSet);

        debug("Updating game: " + game);

        SteamAttributeUpdater steamAttributeUpdater = new SteamAttributeUpdater(game, connection, chromeDriver);
        steamAttributeUpdater.runUpdater();
      } catch (SQLException e) {
        e.printStackTrace();
        debug("Game failed to load from DB.");
      } catch (GameFailedException e) {
        e.printStackTrace();
        debug("Game failed: " + game);
      }

      debug(i + " processed.");
    }

    chromeDriver.close();
  }


  protected void debug(Object object) {
    System.out.println(object);
  }
}

