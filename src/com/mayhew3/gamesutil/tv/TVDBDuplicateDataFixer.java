package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.dataobject.EpisodePostgres;
import com.mayhew3.gamesutil.dataobject.TVDBEpisodePostgres;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.sun.istack.internal.Nullable;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TVDBDuplicateDataFixer {
  private SQLConnection connection;

  public TVDBDuplicateDataFixer(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String[] args) throws URISyntaxException, SQLException {
    SQLConnection connection = new PostgresConnectionFactory().createLocalConnection();
    TVDBDuplicateDataFixer dataFixer = new TVDBDuplicateDataFixer(connection);
    dataFixer.runUpdate();
  }

  private void runUpdate() throws SQLException {
    List<ShowFailedException> exceptions = new ArrayList<>();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT e.seriesid, e.season, e.season_episode_number\n" +
            "FROM episode e\n" +
            "WHERE retired = ? " +
            "AND season <> ? " +
            "GROUP BY e.seriesid, e.series_title, e.season, e.season_episode_number\n" +
            "HAVING count(1) > ?\n" +
            "ORDER BY e.series_title, e.season, e.season_episode_number", 0, 0, 1);

    while (resultSet.next()) {
      Integer seriesid = resultSet.getInt("seriesid");
      Integer season = resultSet.getInt("season");
      Integer seasonEpisodeNumber = resultSet.getInt("season_episode_number");

      try {
        resolveDuplicatesForEpisode(seriesid, season, seasonEpisodeNumber);
      } catch (ShowFailedException e) {
        e.printStackTrace();
        exceptions.add(e);
      }
    }

    debug("Finished.");

    debug("Shows failed: " + exceptions.size());
    exceptions.forEach(this::debug);
  }

  private void resolveDuplicatesForEpisode(Integer seriesid, Integer season, Integer seasonEpisodeNumber) throws SQLException, ShowFailedException {
    debug("- SeriesID " + seriesid + ", Season " + season + ", Episode " + seasonEpisodeNumber);

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM episode " +
            "WHERE seriesid = ? " +
            "AND season = ? " +
            "AND season_episode_number = ? " +
            "AND retired = ? ", seriesid, season, seasonEpisodeNumber, 0);

    List<EpisodePostgres> olderEpisodes = new ArrayList<>();

    while (resultSet.next()) {
      EpisodePostgres episode = new EpisodePostgres();
      episode.initializeFromDBObject(resultSet);

      olderEpisodes.add(episode);
    }

    debug("  - " + olderEpisodes.size() + " duplicates.");

    EpisodePostgres mostRecentEpisode = findMostRecentEpisode(olderEpisodes);

    if (mostRecentEpisode == null) {
      throw new ShowFailedException("No episodes with DateAdded field: SeriesID " + seriesid + ", Season " + season + ", Episode " + seasonEpisodeNumber);
    }

    debug("  - Episode " + mostRecentEpisode.id.getValue() + " chosen with DateAdded " + mostRecentEpisode.dateAdded.getValue());

    olderEpisodes.remove(mostRecentEpisode);

    for (EpisodePostgres episode : olderEpisodes) {
      episode.retired.changeValue(episode.id.getValue());
      episode.commit(connection);

      TVDBEpisodePostgres tvdbEpisode = getTVDBEpisode(episode);
      tvdbEpisode.retired.changeValue(tvdbEpisode.id.getValue());
      tvdbEpisode.commit(connection);
    }

    debug("  - " + olderEpisodes.size() + " episodes and tvdb_episodes removed.");
  }

  @Nullable
  private EpisodePostgres findMostRecentEpisode(List<EpisodePostgres> episodes) {
    EpisodePostgres mostRecent = null;
    for (EpisodePostgres episode : episodes) {
      Timestamp dateAdded = episode.dateAdded.getValue();
      if (dateAdded != null) {
        if (mostRecent == null || dateAdded.after(mostRecent.dateAdded.getValue())) {
          mostRecent = episode;
        }
      }
    }
    return mostRecent;
  }

  private TVDBEpisodePostgres getTVDBEpisode(EpisodePostgres episode) throws SQLException, ShowFailedException {
    Integer tvdbEpisodeId = episode.tvdbEpisodeId.getValue();
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch("SELECT * FROM tvdb_episode WHERE id = ? AND retired = ?", tvdbEpisodeId, 0);

    if (resultSet.next()) {
      TVDBEpisodePostgres tvdbEpisode = new TVDBEpisodePostgres();
      tvdbEpisode.initializeFromDBObject(resultSet);
      return tvdbEpisode;
    } else {
      throw new ShowFailedException("Episode " + episode.id.getValue() + " has tvdb_episode_id " + tvdbEpisodeId + " that wasn't found.");
    }
  }

  protected void debug(Object object) {
    System.out.println(object);
  }

}
