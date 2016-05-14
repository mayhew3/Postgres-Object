package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.model.tv.Episode;
import com.mayhew3.gamesutil.model.tv.Series;
import com.mayhew3.gamesutil.model.tv.TVDBEpisode;
import com.mayhew3.gamesutil.model.tv.TiVoEpisode;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.NodeReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.w3c.dom.NodeList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

class TVDBEpisodeUpdater {
  enum EPISODE_RESULT {ADDED, UPDATED, NONE}

  private Series series;
  private NodeList episodeNode;
  private SQLConnection connection;
  private NodeReader nodeReader;

  public TVDBEpisodeUpdater(Series series, NodeList episodeNode, SQLConnection connection, NodeReader nodeReader) {
    this.series = series;
    this.connection = connection;
    this.nodeReader = nodeReader;
    this.episodeNode = episodeNode;
  }

  /**
   * @return Whether a new episode was added, or episode was found and updated.
   * @throws SQLException If DB query error
   * @throws ShowFailedException If multiple episodes were found to update
   */
  EPISODE_RESULT updateSingleEpisode() throws SQLException, ShowFailedException, BadlyFormattedXMLException {
    Integer tvdbRemoteId = Integer.valueOf(nodeReader.getValueOfSimpleStringNode(episodeNode, "id"));

    Integer seriesId = series.id.getValue();

    TVDBEpisode existingTVDBEpisodeByTVDBID = findExistingTVDBEpisodeByTVDBID(tvdbRemoteId);

    @NotNull String episodenumber = nodeReader.getValueOfSimpleStringNode(episodeNode, "episodenumber");
    @NotNull String episodename = nodeReader.getValueOfSimpleStringNode(episodeNode, "episodename");
    @NotNull String seasonnumber = nodeReader.getValueOfSimpleStringNode(episodeNode, "seasonnumber");
    @NotNull String firstaired = nodeReader.getValueOfSimpleStringNode(episodeNode, "firstaired");

    TVDBEpisode existingTVDBEpisodeByEpisodeNumber = findExistingTVDBEpisodeByEpisodeNumber(
        Integer.valueOf(episodenumber),
        Integer.valueOf(seasonnumber),
        series.tvdbSeriesId.getValue());

    TVDBEpisode existingEpisode = existingTVDBEpisodeByTVDBID == null ?
        existingTVDBEpisodeByEpisodeNumber :
        existingTVDBEpisodeByTVDBID;

    Boolean matched = false;
    Boolean added = false;
    Boolean changed = false;

    TVDBEpisode tvdbEpisode = new TVDBEpisode();
    Episode episode = new Episode();

    if (existingEpisode == null) {
      tvdbEpisode.initializeForInsert();

      // todo: Optimization: skip looking for match when firstAired is future. Obviously it's not on the TiVo yet.
      TiVoEpisode tivoEpisode = findTiVoMatch(episodename, seasonnumber, episodenumber, firstaired, seriesId);

      if (tivoEpisode == null) {
        episode.initializeForInsert();
        added = true;
      } else {

        // todo: handle multiple rows returned MM-11
        ResultSet episodeRow = getEpisodeFromTiVoEpisodeID(tivoEpisode.id.getValue());
        episode.initializeFromDBObject(episodeRow);
        matched = true;
      }

    } else {
      tvdbEpisode = existingEpisode;

      ResultSet episodeRow = getEpisodeFromTVDBEpisodeID(tvdbEpisode.id.getValue());
      episode.initializeFromDBObject(episodeRow);
    }

    // todo: Add log entry for when TVDB values change.

    String absoluteNumber = nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "absoute_number");

    tvdbEpisode.tvdbEpisodeExtId.changeValue(tvdbRemoteId);
    tvdbEpisode.absoluteNumber.changeValueFromString(absoluteNumber);
    tvdbEpisode.seasonNumber.changeValueFromString(seasonnumber);
    tvdbEpisode.episodeNumber.changeValueFromString(episodenumber);
    tvdbEpisode.name.changeValueFromString(episodename);
    tvdbEpisode.firstAired.changeValueFromString(firstaired);
    tvdbEpisode.tvdbSeriesId.changeValue(series.tvdbSeriesId.getValue());
    tvdbEpisode.overview.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "overview"));
    tvdbEpisode.productionCode.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "ProductionCode"));
    tvdbEpisode.rating.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "Rating"));
    tvdbEpisode.ratingCount.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "RatingCount"));
    tvdbEpisode.director.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "Director"));
    tvdbEpisode.writer.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "Writer"));
    tvdbEpisode.lastUpdated.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "lastupdated"));
    tvdbEpisode.tvdbSeasonExtId.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "seasonid"));
    tvdbEpisode.filename.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "filename"));
    tvdbEpisode.airsAfterSeason.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "airsafter_season"));
    tvdbEpisode.airsBeforeSeason.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "airsbefore_season"));
    tvdbEpisode.airsBeforeEpisode.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "airsbefore_episode"));
    tvdbEpisode.thumbHeight.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "thumb_height"));
    tvdbEpisode.thumbWidth.changeValueFromString(nodeReader.getValueOfSimpleStringNullableNode(episodeNode, "thumb_width"));

    if (tvdbEpisode.hasChanged()) {
      changed = true;
    }
    tvdbEpisode.commit(connection);

    episode.seriesId.changeValue(seriesId);
    episode.seriesTitle.changeValueFromString(series.seriesTitle.getValue());
    episode.tvdbEpisodeId.changeValue(tvdbEpisode.id.getValue());
    episode.title.changeValue(episodename);
    episode.setSeasonFromString(seasonnumber, connection);
    episode.absoluteNumber.changeValueFromString(absoluteNumber);
    episode.episodeNumber.changeValueFromString(episodenumber);
    episode.airDate.changeValueFromString(firstaired);
    episode.streaming.changeValue(series.isStreaming(connection));

    if (episode.hasChanged()) {
      changed = true;
    }
    episode.commit(connection);

    Integer episodeId = tvdbEpisode.id.getValue();

    if (episodeId == null) {
      throw new RuntimeException("_id wasn't populated on Episode with tvdbEpisodeId " + tvdbRemoteId + " after insert.");
    } else {
      // add manual reference to episode to episodes array.

      updateSeriesDenorms(added, matched, series);

      series.commit(connection);
    }

    if (added) {
      return EPISODE_RESULT.ADDED;
    } else if (changed) {
      return EPISODE_RESULT.UPDATED;
    } else {
      return EPISODE_RESULT.NONE;
    }
  }

  @Nullable
  private TVDBEpisode findExistingTVDBEpisodeByTVDBID(Integer tvdbId) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM tvdb_episode " +
            "WHERE tvdb_episode_ext_id = ? " +
            "AND retired = ?",
        tvdbId, 0
    );
    if (resultSet.next()) {
      TVDBEpisode tvdbEpisode = new TVDBEpisode();
      tvdbEpisode.initializeFromDBObject(resultSet);
      return tvdbEpisode;
    } else {
      return null;
    }
  }

  @Nullable
  private TVDBEpisode findExistingTVDBEpisodeByEpisodeNumber(Integer episodeNumber, Integer seasonNumber, Integer tvdbSeriesId) throws SQLException, ShowFailedException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT te.* " +
            "FROM tvdb_episode te " +
            "WHERE tvdb_series_id = ? " +
            "AND episode_number = ? " +
            "AND season_number = ? " +
            "AND retired = ?",
        tvdbSeriesId, episodeNumber, seasonNumber, 0
    );
    if (resultSet.next()) {
      TVDBEpisode tvdbEpisode = new TVDBEpisode();
      tvdbEpisode.initializeFromDBObject(resultSet);

      if (resultSet.next()) {
        throw new ShowFailedException("Found multiple matches for Series '" + tvdbEpisode.seriesName.getValue() + "' (" +
            tvdbSeriesId + "), " + seasonNumber + "x" + episodeNumber);
      }

      return tvdbEpisode;
    } else {
      return null;
    }
  }

  @Nullable
  private TiVoEpisode findTiVoMatch(String episodeTitle, String tvdbSeasonStr, String tvdbEpisodeNumberStr, String firstAiredStr, Integer seriesId) throws SQLException {
    List<TiVoEpisode> matchingEpisodes = new ArrayList<>();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT te.* " +
            "FROM tivo_episode te " +
            "INNER JOIN edge_tivo_episode ete " +
            "  ON ete.tivo_episode_id = te.id " +
            "INNER JOIN episode e " +
            "  ON ete.episode_id = e.id " +
            "WHERE e.series_id = ? " +
            "AND e.tvdb_episode_id IS NULL " +
            "AND e.retired = ? " +
            "AND te.retired = ? ",
        seriesId, 0, 0
    );

    List<TiVoEpisode> episodes = new ArrayList<>();

    while(resultSet.next()) {
      TiVoEpisode episode = new TiVoEpisode();
      episode.initializeFromDBObject(resultSet);
      episodes.add(episode);
    }

    if (episodeTitle != null) {
      for (TiVoEpisode episode : episodes) {
        String tivoTitleObject = episode.title.getValue();
        if (episodeTitle.equalsIgnoreCase(tivoTitleObject)) {
          matchingEpisodes.add(episode);
        }
      }
    }

    if (matchingEpisodes.size() == 1) {
      return matchingEpisodes.get(0);
    } else if (matchingEpisodes.size() > 1) {
      debug("Found " + matchingEpisodes.size() + " matching episodes for " +
          tvdbSeasonStr + "x" + tvdbEpisodeNumberStr + " " +
          "'" + episodeTitle + "'.");
      return null;
    }

    // no match found on episode title. Try episode number.


    if (tvdbEpisodeNumberStr != null && tvdbSeasonStr != null) {
      Integer tvdbSeason = Integer.valueOf(tvdbSeasonStr);
      Integer tvdbEpisodeNumber = Integer.valueOf(tvdbEpisodeNumberStr);

      for (TiVoEpisode episode : episodes) {

        Integer tivoEpisodeNumber = episode.episodeNumber.getValue();

        if (tivoEpisodeNumber != null) {

          Integer tivoSeasonNumber = 1;

          if (tivoEpisodeNumber < 100) {
            if (Objects.equals(tivoSeasonNumber, tvdbSeason) && Objects.equals(tivoEpisodeNumber, tvdbEpisodeNumber)) {
              matchingEpisodes.add(episode);
            }
          } else {
            String tiVoEpisodeNumberStr = tivoEpisodeNumber.toString();
            int seasonLength = tiVoEpisodeNumberStr.length() / 2;

            String tivoSeasonStr = tiVoEpisodeNumberStr.substring(0, seasonLength);
            String tivoEpisodeNumberStr = tiVoEpisodeNumberStr.substring(seasonLength, tiVoEpisodeNumberStr.length());

            tivoEpisodeNumber = Integer.valueOf(tivoEpisodeNumberStr);
            tivoSeasonNumber = Integer.valueOf(tivoSeasonStr);

            if (Objects.equals(tivoSeasonNumber, tvdbSeason) && Objects.equals(tivoEpisodeNumber, tvdbEpisodeNumber)) {
              matchingEpisodes.add(episode);
            }
          }
        }
      }

    }


    if (matchingEpisodes.size() == 1) {
      return matchingEpisodes.get(0);
    } else if (matchingEpisodes.size() > 1) {
      debug("Found " + matchingEpisodes.size() + " matching episodes for " +
          tvdbSeasonStr + "x" + tvdbEpisodeNumberStr + " " +
          "'" + episodeTitle + "'.");
      return null;
    }


    // no match on episode number. Try air date.

    if (firstAiredStr != null) {
      DateTime firstAired = new DateTime(firstAiredStr);

      for (TiVoEpisode episode : episodes) {
        Date showingStartTimeObj = episode.showingStartTime.getValue();

        if (showingStartTimeObj != null) {
          DateTime showingStartTime = new DateTime(showingStartTimeObj);

          DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();

          if (comparator.compare(firstAired, showingStartTime) == 0) {
            matchingEpisodes.add(episode);
          }
        }
      }

    }


    if (matchingEpisodes.size() == 1) {
      return matchingEpisodes.get(0);
    } else if (matchingEpisodes.size() > 1) {
      debug("Found " + matchingEpisodes.size() + " matching episodes for " +
          tvdbSeasonStr + "x" + tvdbEpisodeNumberStr + " " +
          "'" + episodeTitle + "'.");
      return null;
    } else {
      debug("Found no matches for " +
          tvdbSeasonStr + "x" + tvdbEpisodeNumberStr + " " +
          "'" + episodeTitle + "'.");
      return null;
    }

  }

  // todo: handle returning multiple episodes MM-11
  private ResultSet getEpisodeFromTiVoEpisodeID(Integer tivoEpisodeID) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT e.* " +
            "FROM episode e " +
            "INNER edge_tivo_episode ete " +
            "  ON ete.episode_id = e.id " +
            "WHERE ete.tivo_episode_id = ? " +
            "AND e.retired = ? ",
        tivoEpisodeID, 0
    );
    if (!resultSet.next()) {
      throw new RuntimeException("No row in episode matching tivo_episode_id " + tivoEpisodeID);
    }

    return resultSet;
  }

  private ResultSet getEpisodeFromTVDBEpisodeID(Integer tvdbEpisodeID) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM episode " +
            "WHERE tvdb_episode_id = ?",
        tvdbEpisodeID
    );
    if (!resultSet.next()) {
      throw new RuntimeException("No row in episode matching tvdb_episode_id " + tvdbEpisodeID);
    }

    return resultSet;
  }

  private void updateSeriesDenorms(Boolean added, Boolean matched, Series series) {
    if (added) {
      series.tvdbOnlyEpisodes.increment(1);
      series.unwatchedUnrecorded.increment(1);
    }
    if (matched) {
      series.matchedEpisodes.increment(1);
      series.unmatchedEpisodes.increment(-1);
    }
  }

  protected void debug(Object object) {
    System.out.println(object);
  }

}
