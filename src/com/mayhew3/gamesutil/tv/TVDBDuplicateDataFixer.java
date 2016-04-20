package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Ordering;
import com.mayhew3.gamesutil.dataobject.EpisodePostgres;
import com.mayhew3.gamesutil.dataobject.SeriesPostgres;
import com.mayhew3.gamesutil.dataobject.TVDBEpisodePostgres;
import com.mayhew3.gamesutil.dataobject.TiVoEpisodePostgres;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang3.ObjectUtils;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class TVDBDuplicateDataFixer {
  private SQLConnection connection;

  private final Ordering<EpisodePostgres> DATEADDED = new Ordering<EpisodePostgres>() {
    @Override
    public int compare(@NotNull EpisodePostgres episode1, @NotNull EpisodePostgres episode2) {
      return Objects.compare(episode1.dateAdded.getValue(), episode2.dateAdded.getValue(),
          (o1, o2) -> ObjectUtils.compare(o1, o2, false));
    }
  };

  private final Ordering<EpisodePostgres> DATEADDED_REVERSE = new Ordering<EpisodePostgres>() {
    @Override
    public int compare(@NotNull EpisodePostgres episode1, @NotNull EpisodePostgres episode2) {
      return Objects.compare(episode1.dateAdded.getValue(), episode2.dateAdded.getValue(),
          (o1, o2) -> ObjectUtils.compare(o1, o2, true));
    }
  };

  private final Ordering<EpisodePostgres> WATCHEDDATE = new Ordering<EpisodePostgres>() {
    @Override
    public int compare(@NotNull EpisodePostgres episode1, @NotNull EpisodePostgres episode2) {
      return Objects.compare(episode1.watchedDate.getValue(), episode2.watchedDate.getValue(),
          (o1, o2) -> ObjectUtils.compare(o1, o2, false));
    }
  };

  private final Ordering<EpisodePostgres> ONTIVO = new Ordering<EpisodePostgres>() {
    @Override
    public int compare(@NotNull EpisodePostgres episode1, @NotNull EpisodePostgres episode2) {
      return Objects.compare(episode1.onTiVo.getValue(), episode2.onTiVo.getValue(),
          (o1, o2) -> ObjectUtils.compare(o1, o2, false));
    }
  };

  private final Ordering<EpisodePostgres> WATCHED = new Ordering<EpisodePostgres>() {
    @Override
    public int compare(@NotNull EpisodePostgres episode1, @NotNull EpisodePostgres episode2) {
      return Objects.compare(episode1.watched.getValue(), episode2.watched.getValue(),
          (o1, o2) -> ObjectUtils.compare(o1, o2, false));
    }
  };



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
    Set<TiVoEpisodePostgres> tivoEpisodes = new HashSet<>();

    while (resultSet.next()) {
      EpisodePostgres episode = new EpisodePostgres();
      episode.initializeFromDBObject(resultSet);

      olderEpisodes.add(episode);
      tivoEpisodes.addAll(episode.getTiVoEpisodes(connection));
    }

    if (tivoEpisodes.size() > 1) {
      for (TiVoEpisodePostgres tivoEpisode : tivoEpisodes) {
        if (unlinkIncorrectEpisodes(tivoEpisode)) {
          tivoEpisodes.remove(tivoEpisode);
        }
      }
    }

    if (tivoEpisodes.size() > 1) {
      throw new ShowFailedException("Multiple tivo_episodes associated with various duplicate episode rows!");
    }

    Boolean watched = WATCHED.max(olderEpisodes).watched.getValue();
    Timestamp watchedDate = WATCHEDDATE.max(olderEpisodes).watchedDate.getValue();
    Boolean onTivo = ONTIVO.max(olderEpisodes).onTiVo.getValue();
    Timestamp dateAdded = DATEADDED_REVERSE.min(olderEpisodes).dateAdded.getValue();

    debug("  - " + olderEpisodes.size() + " duplicates.");

    EpisodePostgres mostRecentEpisode = DATEADDED.max(olderEpisodes);

    if (dateAdded == null) {
      throw new ShowFailedException("No episodes with DateAdded field: SeriesID " + seriesid + ", Season " + season + ", Episode " + seasonEpisodeNumber);
    }

    debug("  - Episode " + mostRecentEpisode.id.getValue() + " chosen with DateAdded " + mostRecentEpisode.dateAdded.getValue());

    olderEpisodes.remove(mostRecentEpisode);

    for (EpisodePostgres episode : olderEpisodes) {
      if (!tivoEpisodes.isEmpty()) {
        unlinkAllTiVoEpisodes(episode);
      }

      episode.retired.changeValue(episode.id.getValue());
      episode.commit(connection);

      TVDBEpisodePostgres tvdbEpisode = episode.getTVDBEpisode(connection);
      tvdbEpisode.retired.changeValue(tvdbEpisode.id.getValue());
      tvdbEpisode.commit(connection);
    }

    debug("  - " + olderEpisodes.size() + " episodes and tvdb_episodes removed.");

    mostRecentEpisode.watched.changeValue(watched);
    mostRecentEpisode.watchedDate.changeValue(watchedDate);
    mostRecentEpisode.onTiVo.changeValue(onTivo);
    mostRecentEpisode.dateAdded.changeValue(dateAdded);
    mostRecentEpisode.commit(connection);

    for (TiVoEpisodePostgres tivoEpisode : tivoEpisodes) {
      mostRecentEpisode.addToTiVoEpisodes(connection, tivoEpisode.id.getValue());
    }

    TVDBEpisodePostgres mostRecentTVDBEpisode = mostRecentEpisode.getTVDBEpisode(connection);
    mostRecentTVDBEpisode.dateAdded.changeValue(dateAdded);
    mostRecentTVDBEpisode.commit(connection);
  }

  private void unlinkAllTiVoEpisodes(EpisodePostgres episode) throws SQLException {
    connection.prepareAndExecuteStatementUpdate("" +
        "UPDATE edge_tivo_episode " +
        "SET retired = id " +
        "WHERE episode_id = ?", episode.id.getValue());
  }

  private Boolean unlinkIncorrectEpisodes(TiVoEpisodePostgres tiVoEpisode) throws SQLException, ShowFailedException {
    List<EpisodePostgres> episodes = tiVoEpisode.getEpisodes(connection);
    if (episodes.size() > 1) {
      throw new ShowFailedException("Don't know how to handle multi-episode recording yet: " +
          tiVoEpisode.seriesTitle.getValue() + ": " + tiVoEpisode.episodeNumber.getValue() + " " +
          tiVoEpisode.title.getValue());
    }

    EpisodePostgres episode = episodes.get(0);

    if (betterMatchExists(tiVoEpisode, episode)) {
      connection.prepareAndExecuteStatementUpdate(
          "UPDATE edge_tivo_episode " +
              "SET retired = id " +
              "WHERE tivo_episode_id = ?", tiVoEpisode.id.getValue()
      );
      return true;
    }
    return false;
  }

  private Boolean betterMatchExists(TiVoEpisodePostgres tiVoEpisodePostgres, EpisodePostgres currentlyMatched) throws ShowFailedException, SQLException {
    if (tiVoEpisodePostgres.title.getValue().equals(currentlyMatched.title.getValue())) {
      return false;
    } else {
      SeriesPostgres series = currentlyMatched.getSeries(connection);
      List<EpisodePostgres> otherSeriesEpisodes = series.getEpisodes(connection);
      otherSeriesEpisodes.remove(currentlyMatched);

      for (EpisodePostgres episode : otherSeriesEpisodes) {
        if (tiVoEpisodePostgres.title.getValue().equals(episode.title.getValue())) {
          return true;
        }
      }
    }
    return false;
  }



  protected void debug(Object object) {
    System.out.println(object);
  }

}
