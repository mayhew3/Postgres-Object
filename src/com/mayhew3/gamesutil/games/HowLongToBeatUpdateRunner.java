package com.mayhew3.gamesutil.games;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.mediaobject.Game;
import org.joda.time.DateTime;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class HowLongToBeatUpdateRunner {

  private SQLConnection connection;

  public HowLongToBeatUpdateRunner(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String[] args) throws FileNotFoundException, SQLException, URISyntaxException {
    List<String> argList = Lists.newArrayList(args);
    Boolean logToFile = argList.contains("LogToFile");

    if (logToFile) {
      File file = new File("D:\\Projects\\mean_projects\\GamesDBUtil\\logs\\HowLongToBeatUpdater.log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);
      System.setOut(ps);
    }

    SQLConnection connection = new PostgresConnectionFactory().createConnection();

    HowLongToBeatUpdateRunner updateRunner = new HowLongToBeatUpdateRunner(connection);

    updateRunner.runUpdate();

  }

  public void runUpdate() throws SQLException {
    Date date = new DateTime().minusDays(7).toDate();
    Timestamp timestamp = new Timestamp(date.getTime());

    String sql = "SELECT * FROM games WHERE howlong_updated IS NULL " +
        " AND (howlong_failed IS NULL OR howlong_failed < ?)";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, timestamp);

    int i = 1;
    int failures = 0;

    ChromeDriver chromeDriver = new ChromeDriver();

    while (resultSet.next()) {
      Game game = new Game();
      try {
        game.initializeFromDBObject(resultSet);

        debug("Updating game: " + game);

        HowLongToBeatUpdater updater = new HowLongToBeatUpdater(game, connection, chromeDriver);
        updater.runUpdater();
      } catch (SQLException e) {
        e.printStackTrace();
        debug("Game failed to load from DB.");
        failures++;
      } catch (GameFailedException e) {
        e.printStackTrace();
        debug("Game failed: " + game);
        logFailure(game);
        failures++;
      } catch (WebDriverException e) {
        e.printStackTrace();
        debug("WebDriver error: " + game);
        logFailure(game);
        failures++;
      }

      debug(i + " processed.");
      i++;
    }

    debug("Operation completed! Failed on " + failures + "/" + (i-1) + " games (" + (failures/i*100) + "%)");

    chromeDriver.close();
  }

  private void logFailure(Game game) throws SQLException {
    game.howlong_failed.changeValue(new Timestamp(new Date().getTime()));
    game.commit(connection);
  }


  protected void debug(Object object) {
    System.out.println(object);
  }
}

