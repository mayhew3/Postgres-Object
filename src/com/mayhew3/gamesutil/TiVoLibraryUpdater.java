package com.mayhew3.gamesutil;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;

import java.net.UnknownHostException;
import java.util.List;

public class TiVoLibraryUpdater {


  public static void main(String[] args) {
    List<String> argList = Lists.newArrayList(args);
    Boolean lookAtAllShows = argList.contains("FullMode");
    Boolean tvdbOnly = argList.contains("TVDBOnly");
    Boolean tiVoOnly = argList.contains("TiVoOnly");


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
      logger.logConnectionEnd(new BasicDBObject());
      logger.closeDatabase();
      throw new RuntimeException("Error connecting to TiVo database.");
    }

    if (!tvdbOnly) {
      try {
        tiVoCommunicator.runUpdate(lookAtAllShows);
      } catch (RuntimeException e) {
        e.printStackTrace();
        logger.logConnectionEnd(tiVoCommunicator.getSessionInfo());
        logger.closeDatabase();
        throw new RuntimeException("Error downloading info from TiVo box!");
      }
    }

    TVDBUpdateRunner tvdbUpdateRunner;
    try {
      tvdbUpdateRunner = new TVDBUpdateRunner();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      logger.logConnectionEnd(tiVoCommunicator.getSessionInfo());
      logger.closeDatabase();
      throw new RuntimeException("Error connecting to TiVo database while updating TVDB info.");
    }

    if (!tiVoOnly) {
      try {
        tvdbUpdateRunner.runUpdate();
      } catch (RuntimeException e) {
        e.printStackTrace();
        logger.logConnectionEnd(tvdbUpdateRunner.getSessionInfo());
        logger.closeDatabase();
        throw new RuntimeException("Error downloading info from TVDB service.");
      }
    }

    logger.logConnectionEnd(tiVoCommunicator.getSessionInfo());
    logger.logConnectionEnd(tvdbUpdateRunner.getSessionInfo());
    logger.closeDatabase();

  }

  protected static void debug(Object object) {
    System.out.println(object);
  }

}
