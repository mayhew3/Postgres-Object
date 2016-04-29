package com.mayhew3.gamesutil.dataobject;

import com.mayhew3.gamesutil.db.SQLConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TiVoEpisode extends DataObject {

  public FieldValue<Boolean> suggestion = registerBooleanField("suggestion");

  public FieldValueString title = registerStringField("title");

  public FieldValueTimestamp showingStartTime = registerTimestampField("showing_start_time");
  public FieldValueTimestamp deletedDate = registerTimestampField("deleted_date");
  public FieldValueTimestamp captureDate = registerTimestampField("capture_date");

  public FieldValue<Boolean> hd = registerBooleanField("hd");

  public FieldValue<Integer> episodeNumber = registerIntegerField("episode_number");
  public FieldValue<Integer> duration = registerIntegerField("duration");
  public FieldValue<Integer> showingDuration = registerIntegerField("showing_duration");
  public FieldValue<Integer> channel = registerIntegerField("channel");
  public FieldValue<Integer> rating = registerIntegerField("rating");

  public FieldValue<String> tivoSeriesId = registerStringField("tivo_series_id");
  public FieldValue<String> programId = registerStringField("program_id");
  public FieldValue<String> seriesTitle = registerStringField("series_title");
  public FieldValue<String> description = registerStringField("description");
  public FieldValue<String> station = registerStringField("station");
  public FieldValue<String> url = registerStringField("url");

  public FieldValueInteger retired = registerIntegerField("retired");

  @Override
  protected String getTableName() {
    return "tivo_episode";
  }

  @Override
  public String toString() {
    return seriesTitle.getValue() + " " + episodeNumber.getValue() + ": " + title.getValue();
  }

  public List<Episode> getEpisodes(SQLConnection connection) throws SQLException {
    List<Episode> episodes = new ArrayList<>();
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT e.* " +
            "FROM episode e " +
            "INNER JOIN edge_tivo_episode ete " +
            " ON ete.episode_id = e.id " +
            "WHERE ete.tivo_episode_id = ? " +
            "AND e.retired = ? ", id.getValue(), 0);

    while (resultSet.next()) {
      Episode episode = new Episode();
      episode.initializeFromDBObject(resultSet);
      episodes.add(episode);
    }
    return episodes;
  }
}
