package com.mayhew3.gamesutil;

import com.google.common.collect.Lists;

import java.net.UnknownHostException;
import java.util.List;

public class TiVoLibraryUpdater {


  public static void main(String[] args) {
    Boolean lookAtAllShows = false;

    List<String> argList = Lists.newArrayList(args);
    if (argList.contains("FullMode")) {
      lookAtAllShows = true;
    }


    ConnectionLogger logger;
    try {
      logger = new ConnectionLogger();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      throw new RuntimeException("Error connecting to DB for logging.");
    }


    logger.logConnectionStart(lookAtAllShows);

    TiVoCommunicator tiVoCommunicator;
    try {
      tiVoCommunicator = new TiVoCommunicator();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      throw new RuntimeException("Error connecting to TiVo database.");
    }

    try {
      tiVoCommunicator.runUpdate(lookAtAllShows);
    } catch (RuntimeException e) {
      e.printStackTrace();
    } finally {
      logger.logConnectionEnd(tiVoCommunicator.getSessionInfo());
      logger.closeDatabase();
    }



  }

  protected static void debug(Object object) {
    System.out.println(object);
  }

}
