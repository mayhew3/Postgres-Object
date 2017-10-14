package com.mayhew3.mediamogul.games;

import com.google.common.collect.Lists;
import com.mayhew3.mediamogul.ArgumentChecker;
import com.mayhew3.mediamogul.scheduler.UpdateRunner;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.model.games.Game;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SteamAttributeUpdateRunner implements UpdateRunner {

  private SQLConnection connection;

  public SteamAttributeUpdateRunner(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String[] args) throws FileNotFoundException, SQLException, URISyntaxException {
    List<String> argList = Lists.newArrayList(args);
    Boolean logToFile = argList.contains("LogToFile");
    Boolean redo = false;
    ArgumentChecker argumentChecker = new ArgumentChecker(args);

    if (logToFile) {
      String mediaMogulLogs = System.getenv("MediaMogulLogs");

      File file = new File(mediaMogulLogs + "\\SteamAttributeUpdater.log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);
      System.setOut(ps);
    }

    SQLConnection connection = PostgresConnectionFactory.createConnection(argumentChecker);

    // don't do this.
    if (redo) {
      connection.executeUpdate("TRUNCATE TABLE steam_attributes");
      connection.executeUpdate("ALTER SEQUENCE steam_attributes_id_seq RESTART WITH 1");
      connection.executeUpdate("UPDATE games SET steam_attributes = NULL");
    }

    setDriverPath();

    SteamAttributeUpdateRunner updateRunner = new SteamAttributeUpdateRunner(connection);

    updateRunner.runUpdate();

  }

  public void runUpdate() throws SQLException {
    String sql = "SELECT * FROM games WHERE steamid is not null AND steam_attributes IS NULL AND steam_page_gone IS NULL";
    ResultSet resultSet = connection.executeQuery(sql);

    int i = 0;

    ChromeDriver chromeDriver = new ChromeDriver();

    while (resultSet.next()) {
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

  private static void setDriverPath() {
    String driverPath = System.getProperty("user.dir") + "\\resources\\chromedriver.exe";
    System.setProperty("webdriver.chrome.driver", driverPath);
  }


  protected void debug(Object object) {
    System.out.println(object);
  }

  @Override
  public String getRunnerName() {
    return "Steam Attribute Update Runner";
  }
}

