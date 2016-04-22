package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;

import java.net.URISyntaxException;
import java.sql.SQLException;

public class SeriesDenormUpdater {

  private SQLConnection connection;

  public SeriesDenormUpdater(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String[] args) throws URISyntaxException, SQLException {
    SQLConnection connection = new PostgresConnectionFactory().createLocalConnection();

    SeriesDenormUpdater updater = new SeriesDenormUpdater(connection);
    updater.updateFields();
  }

  public void updateFields() throws SQLException {
    updateUnmatchedEpisodes();
    updateActiveEpisodes();
    updateUnwatchedEpisodes();
    updateLastUnwatched();
    updateMostRecent();
  }

  private void updateMostRecent() throws SQLException {
    connection.prepareAndExecuteStatementUpdate(
        "update series\n" +
            "set most_recent = (select max(te.showing_start_time)\n" +
            "                            from episode e\n" +
            "                            left outer join edge_tivo_episode ete\n" +
            "                             on ete.episode_id = e.id\n" +
            "                            left outer join tivo_episode te\n" +
            "                             on ete.tivo_episode_id = te.id\n" +
            "                            where e.seriesid = series.id\n" +
            "                            and e.on_tivo = ?\n" +
            "                            and te.suggestion <> ?\n" +
            "                            and te.id is not null\n" +
            "                            and te.deleted_date is null\n" +
            "                            and e.season <> ?\n" +
            "                            and e.retired = ?\n" +
            "                            and te.retired = ?)", true, true, 0, 0, 0
    );
  }

  private void updateLastUnwatched() throws SQLException {
    connection.prepareAndExecuteStatementUpdate(
        "update series\n" +
            "set last_unwatched = (select max(te.showing_start_time)\n" +
            "                            from episode e\n" +
            "                            left outer join edge_tivo_episode ete\n" +
            "                             on ete.episode_id = e.id\n" +
            "                            left outer join tivo_episode te\n" +
            "                             on ete.tivo_episode_id = te.id\n" +
            "                            where e.seriesid = series.id\n" +
            "                            and e.on_tivo = ?\n" +
            "                            and te.suggestion <> ?\n" +
            "                            and te.id is not null\n" +
            "                            and te.deleted_date is null\n" +
            "                            and e.watched <> ?\n" +
            "                            and e.season <> ?\n" +
            "                            and e.retired = ?\n" +
            "                            and te.retired = ?)", true, true, true, 0, 0, 0
    );
  }

  private void updateUnwatchedEpisodes() throws SQLException {
    connection.prepareAndExecuteStatementUpdate(
        "update series s\n" +
            "set unwatched_episodes = (select count(1)\n" +
            "                            from episode e\n" +
            "                            left outer join edge_tivo_episode ete\n" +
            "                             on ete.episode_id = e.id\n" +
            "                            left outer join tivo_episode te\n" +
            "                             on ete.tivo_episode_id = te.id\n" +
            "                            where e.seriesid = s.id\n" +
            "                            and e.on_tivo = ?\n" +
            "                            and te.suggestion <> ?\n" +
            "                            and te.id is not null\n" +
            "                            and te.deleted_date is null\n" +
            "                            and e.watched <> ?\n" +
            "                            and e.season <> ?\n" +
            "                            and e.retired = ?\n" +
            "                            and te.retired = ?)", true, true, true, 0, 0, 0);
  }

  private void updateActiveEpisodes() throws SQLException {
    connection.prepareAndExecuteStatementUpdate(
        "update series\n" +
            "set active_episodes = (select count(1)\n" +
            "                            from episode e\n" +
            "                            left outer join edge_tivo_episode ete\n" +
            "                             on ete.episode_id = e.id\n" +
            "                            left outer join tivo_episode te\n" +
            "                             on ete.tivo_episode_id = te.id\n" +
            "                            where e.seriesid = series.id\n" +
            "                            and e.on_tivo = ?\n" +
            "                            and te.suggestion = ?\n" +
            "                            and te.id is not null\n" +
            "                            and e.season <> ?\n" +
            "                            and te.deleted_date is null\n" +
            "                            and e.retired = ?\n" +
            "                            and te.retired = ?)", true, false, 0, 0, 0);
  }

  private void updateUnmatchedEpisodes() throws SQLException {
    connection.prepareAndExecuteStatementUpdate(
        "update series\n" +
            "set unmatched_episodes = (select count(1)\n" +
            "                            from tivo_episode te\n" +
            "                            where not exists (select 1 from edge_tivo_episode ete where ete.tivo_episode_id = te.id)\n" +
            "                            and te.tivo_series_id = series.tivo_series_id\n" +
            "                            and te.retired = ?)", 0);
  }

}
