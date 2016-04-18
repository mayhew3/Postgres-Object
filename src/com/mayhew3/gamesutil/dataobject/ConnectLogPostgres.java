package com.mayhew3.gamesutil.dataobject;

public class ConnectLogPostgres extends DataObject {

  public FieldValueTimestamp startTime = registerTimestampField("start_time");
  public FieldValueTimestamp endTime = registerTimestampField("end_time");

  public FieldValueInteger addedShows = registerIntegerField("added_shows");
  public FieldValueInteger connectionID = registerIntegerField("connection_id");
  public FieldValueInteger deletedShows = registerIntegerField("deleted_shows");
  public FieldValueInteger tvdbEpisodesAdded = registerIntegerField("tvdb_episodes_added");
  public FieldValueInteger tvdbEpisodesUpdated = registerIntegerField("tvdb_episodes_updated");
  public FieldValueInteger tvdbSeriesUpdated = registerIntegerField("tvdb_series_updated");
  public FieldValueInteger timeConnected = registerIntegerField("time_connected");
  public FieldValueInteger updatedShows = registerIntegerField("updated_shows");

  public FieldValueBoolean fastUpdate = registerBooleanField("fast_update");

  @Override
  protected String getTableName() {
    return "connect_logs";
  }

  @Override
  public String toString() {
   return "Connectlog: " + startTime.getValue() + ", fast: " + fastUpdate.getValue();
  }
}
