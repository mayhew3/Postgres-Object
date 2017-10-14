package com.mayhew3.mediamogul.games;

import com.google.common.collect.Lists;
import com.mayhew3.mediamogul.ArgumentChecker;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;

import java.io.*;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SteamGameUpdateRunner {

  public static void main(String... args) throws SQLException, FileNotFoundException, URISyntaxException, InterruptedException {
    List<String> argList = Lists.newArrayList(args);
    Boolean logToFile = argList.contains("LogToFile");
    ArgumentChecker argumentChecker = new ArgumentChecker(args);

    if (logToFile) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
      String dateFormatted = simpleDateFormat.format(new Date());

      String mediaMogulLogs = System.getenv("MediaMogulLogs");

      File errorFile = new File(mediaMogulLogs + "\\SteamUpdaterErrors_" + dateFormatted + "_" + argumentChecker.getDBIdentifier() + ".log");
      FileOutputStream errorStream = new FileOutputStream(errorFile, true);
      PrintStream ps = new PrintStream(errorStream);
      System.setErr(ps);

      System.err.println("Starting run on " + new Date());

      File logFile = new File(mediaMogulLogs + "\\SteamUpdaterLog_" + dateFormatted + "_" + argumentChecker.getDBIdentifier() + ".log");
      FileOutputStream logStream = new FileOutputStream(logFile, true);
      PrintStream logPrintStream = new PrintStream(logStream);
      System.setOut(logPrintStream);
    }


    debug("");
    debug("SESSION START! Date: " + new Date());
    debug("");

    SQLConnection connection = PostgresConnectionFactory.createConnection(argumentChecker);

    SteamGameUpdater steamGameUpdater = new SteamGameUpdater(connection);
    steamGameUpdater.runUpdate();

    debug(" --- ");
    debug(" Finished Steam API section, starting attribute update!");
    debug(" --- ");

    SteamAttributeUpdateRunner steamAttributeUpdateRunner = new SteamAttributeUpdateRunner(connection);
    steamAttributeUpdateRunner.runUpdate();

    debug(" --- ");
    debug(" Finished Steam Attribute section, starting HowLongToBeat update!");
    debug(" --- ");

    HowLongToBeatUpdateRunner howLongToBeatUpdateRunner = new HowLongToBeatUpdateRunner(connection);
    howLongToBeatUpdateRunner.runUpdate();

    debug(" --- ");
    debug(" Finished HowLongToBeat section, starting GiantBomb update!");
    debug(" --- ");

    GiantBombUpdater giantBombUpdater = new GiantBombUpdater(connection);
    giantBombUpdater.runUpdate();

    debug(" --- ");
    debug(" Full operation complete!");
  }

  protected static void debug(Object object) {
    System.out.println(object);
  }

}
