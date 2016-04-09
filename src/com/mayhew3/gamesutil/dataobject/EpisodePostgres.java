package com.mayhew3.gamesutil.dataobject;

import com.mayhew3.gamesutil.db.SQLConnection;
import com.sun.istack.internal.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EpisodePostgres extends DataObject {

  /* Foreign Keys */
  public FieldValue<Integer> tivoEpisodeId = registerIntegerField("tivo_episode_id");
  public FieldValue<Integer> tvdbEpisodeId = registerIntegerField("tvdb_episode_id");
  public FieldValueInteger seasonId = registerIntegerField("season_id");

  /* Data */
  public FieldValue<Integer> season = registerIntegerField("season");
  public FieldValue<Integer> seasonEpisodeNumber = registerIntegerField("season_episode_number");
  public FieldValue<Integer> episodeNumber = registerIntegerField("episode_number");

  public FieldValueTimestamp airDate = registerTimestampField("air_date");

  public FieldValueInteger seriesId = registerIntegerField("seriesid");

  public FieldValue<Boolean> onTiVo = registerBooleanField("on_tivo");

  public FieldValueString title = registerStringField("title");
  public FieldValueString seriesTitle = registerStringField("series_title");

  public FieldValueString tivoProgramId = registerStringField("tivo_program_id");

  public FieldValueInteger retired = registerIntegerField("retired");

  /* User Data */
  public FieldValueTimestamp watchedDate = registerTimestampField("watched_date");
  public FieldValue<Boolean> watched = registerBooleanField("watched");



  @Override
  protected String getTableName() {
    return "episode";
  }

  @Override
  public String toString() {
    return seriesTitle.getValue() + " " + episodeNumber.getValue() + ": " + title.getValue();
  }

  public void addToTiVoEpisodes(SQLConnection connection, @NotNull Integer tivoLocalEpisodeId) throws SQLException {
    List<TiVoEpisodePostgres> tiVoEpisodes = getTiVoEpisodes(connection);
    if (!hasMatch(tiVoEpisodes, tivoLocalEpisodeId)) {
      EdgeTiVoEpisodePostgres edgeTiVoEpisodePostgres = new EdgeTiVoEpisodePostgres();
      edgeTiVoEpisodePostgres.initializeForInsert();

      edgeTiVoEpisodePostgres.tivoEpisodeId.changeValue(tivoLocalEpisodeId);
      edgeTiVoEpisodePostgres.episodeId.changeValue(id.getValue());
      edgeTiVoEpisodePostgres.retired.changeValue(0);

      edgeTiVoEpisodePostgres.commit(connection);
    }
  }

  private Boolean hasMatch(List<TiVoEpisodePostgres> tiVoEpisodes, Integer tivoLocalEpisodeId) {
    for (TiVoEpisodePostgres tiVoEpisode : tiVoEpisodes) {
      if (tivoLocalEpisodeId.equals(tiVoEpisode.id.getValue())) {
        return true;
      }
    }
    return false;
  }

  public List<TiVoEpisodePostgres> getTiVoEpisodes(SQLConnection connection) throws SQLException {
    String sql = "SELECT te.* " +
        "FROM tivo_episode te " +
        "INNER JOIN edge_tivo_episode e " +
        "  ON e.tivo_episode_id = te.id " +
        "WHERE e.episode_id = ?";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, id.getValue());
    List<TiVoEpisodePostgres> tiVoEpisodePostgresList = new ArrayList<>();

    while (resultSet.next()) {
      TiVoEpisodePostgres tiVoEpisodePostgres = new TiVoEpisodePostgres();
      tiVoEpisodePostgres.initializeFromDBObject(resultSet);

      tiVoEpisodePostgresList.add(tiVoEpisodePostgres);
    }

    return tiVoEpisodePostgresList;
  }
}
