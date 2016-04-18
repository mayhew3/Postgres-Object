package com.mayhew3.gamesutil.dataobject;

public class ConnectLogMongo extends MediaObjectMongoDB {

  public FieldValueDate startTime = registerDateField("StartTime");
  public FieldValueDate endTime = registerDateField("EndTime");

  public FieldValue<Integer> addedShows = registerIntegerField("AddedShows");
  public FieldValue<Integer> connectionID = registerIntegerField("ConnectionID");
  public FieldValue<Integer> deletedShows = registerIntegerField("DeletedShows");
  public FieldValue<Integer> tvdbEpisodesAdded = registerIntegerField("TVDBEpisodesAdded");
  public FieldValue<Integer> tvdbEpisodesUpdated = registerIntegerField("TVDBEpisodesUpdated");
  public FieldValue<Integer> tvdbSeriesUpdated = registerIntegerField("TVDBSeriesUpdated");
  public FieldValue<Long> timeConnected = registerLongField("TimeConnected");
  public FieldValue<Integer> updatedShows = registerIntegerField("UpdatedShows");

  public FieldValue<Boolean> fastUpdate = registerBooleanField("FastUpdate");

  @Override
  protected String getTableName() {
    return "connectlogs";
  }

  @Override
  public String toString() {
   return "Connectlog: " + startTime.getValue() + ", fast: " + fastUpdate.getValue();
  }
}
