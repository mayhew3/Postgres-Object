package com.mayhew3.gamesutil.games;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.DatabaseUtility;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.games.Game;
import com.mayhew3.gamesutil.model.games.GameLog;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class SteamGameUpdateRunner {

  public static void main(String... args) throws SQLException, FileNotFoundException, URISyntaxException, InterruptedException {
    List<String> argList = Lists.newArrayList(args);
    Boolean logToFile = argList.contains("LogToFile");
    String identifier = new ArgumentChecker(args).getDBIdentifier();

    if (logToFile) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
      String dateFormatted = simpleDateFormat.format(new Date());

      String mediaMogulLogs = System.getenv("MediaMogulLogs");

      File errorFile = new File(mediaMogulLogs + "\\SteamUpdaterErrors_" + dateFormatted + "_" + identifier + ".log");
      FileOutputStream errorStream = new FileOutputStream(errorFile, true);
      PrintStream ps = new PrintStream(errorStream);
      System.setErr(ps);

      System.err.println("Starting run on " + new Date());

      File logFile = new File(mediaMogulLogs + "\\SteamUpdaterLog_" + dateFormatted + "_" + identifier + ".log");
      FileOutputStream logStream = new FileOutputStream(logFile, true);
      PrintStream logPrintStream = new PrintStream(logStream);
      System.setOut(logPrintStream);
    }


    debug("");
    debug("SESSION START! Date: " + new Date());
    debug("");

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);

    SteamGameUpdater steamGameUpdater = new SteamGameUpdater(connection);
    steamGameUpdater.updateFields();

    debug(" --- ");
    debug(" Finished Steam API section, starting attribute update!");
    debug(" --- ");

    SteamAttributeUpdateRunner steamAttributeUpdateRunner = new SteamAttributeUpdateRunner(connection);
    steamAttributeUpdateRunner.runSteamAttributeUpdate();

    debug(" --- ");
    debug(" Finished Steam Attribute section, starting HowLongToBeat update!");
    debug(" --- ");

    HowLongToBeatUpdateRunner howLongToBeatUpdateRunner = new HowLongToBeatUpdateRunner(connection);
    howLongToBeatUpdateRunner.runUpdate();

    debug(" --- ");
    debug(" Finished HowLongToBeat section, starting GiantBomb update!");
    debug(" --- ");

    GiantBombUpdater giantBombUpdater = new GiantBombUpdater(connection);
    giantBombUpdater.updateFieldsOnUnmatched();

    debug(" --- ");
    debug(" Full operation complete!");
  }

  protected static void debug(Object object) {
    System.out.println(object);
  }

}
