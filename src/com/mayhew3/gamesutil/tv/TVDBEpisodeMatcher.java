package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.dataobject.TVDBEpisode;
import com.mayhew3.gamesutil.dataobject.TiVoEpisode;
import com.mayhew3.gamesutil.db.SQLConnection;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TVDBEpisodeMatcher {
  private SQLConnection connection;
  private TiVoEpisode tiVoEpisode;
  private Integer seriesId;

  public TVDBEpisodeMatcher(SQLConnection connection, TiVoEpisode tiVoEpisode, Integer seriesId) {
    this.connection = connection;
    this.tiVoEpisode = tiVoEpisode;
    this.seriesId = seriesId;
  }


  public TVDBEpisode findTVDBEpisodeMatch() throws SQLException {
    String episodeTitle = tiVoEpisode.title.getValue();
    Integer episodeNumber = tiVoEpisode.episodeNumber.getValue();
    Date startTime = tiVoEpisode.showingStartTime.getValue();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT te.* " +
            "FROM episode e " +
            "INNER JOIN tvdb_episode te " +
            "  ON e.tvdb_episode_id = te.id " +
            "WHERE NOT EXISTS (SELECT 1 FROM edge_tivo_episode ete WHERE ete.episode_id = e.id) " +
            "AND e.seriesid = ?",
        seriesId
    );

    List<TVDBEpisode> tvdbEpisodes = new ArrayList<>();

    while(resultSet.next()) {
      TVDBEpisode tvdbEpisode = new TVDBEpisode();
      tvdbEpisode.initializeFromDBObject(resultSet);
      tvdbEpisodes.add(tvdbEpisode);
    }

    if (episodeTitle != null) {
      for (TVDBEpisode tvdbEpisode : tvdbEpisodes) {
        String tvdbTitleObject = tvdbEpisode.name.getValue();

        if (episodeTitle.equalsIgnoreCase(tvdbTitleObject)) {
          return tvdbEpisode;
        }
      }
    }

    // no match found on episode title. Try episode number.

    if (episodeNumber != null) {
      Integer seasonNumber = 1;

      if (episodeNumber < 100) {
        TVDBEpisode match = checkForNumberMatch(seasonNumber, episodeNumber, tvdbEpisodes);
        if (match != null) {
          return match;
        }
      } else {
        String episodeNumberString = episodeNumber.toString();
        int seasonLength = episodeNumberString.length() / 2;

        String seasonString = episodeNumberString.substring(0, seasonLength);
        String episodeString = episodeNumberString.substring(seasonLength, episodeNumberString.length());

        TVDBEpisode match = checkForNumberMatch(Integer.valueOf(seasonString), Integer.valueOf(episodeString), tvdbEpisodes);

        if (match != null) {
          return match;
        }
      }
    }

    // no match on episode number. Try air date.

    if (startTime != null) {
      DateTime showingStartTime = new DateTime(startTime);

      for (TVDBEpisode tvdbEpisode : tvdbEpisodes) {
        Date firstAiredValue = tvdbEpisode.firstAired.getValue();
        if (firstAiredValue != null) {
          DateTime firstAired = new DateTime(firstAiredValue);

          DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();

          if (comparator.compare(showingStartTime, firstAired) == 0) {
            return tvdbEpisode;
          }
        }
      }
    }

    return null;
  }

  private TVDBEpisode checkForNumberMatch(Integer seasonNumber, Integer episodeNumber, List<TVDBEpisode> tvdbEpisodes) {
    for (TVDBEpisode tvdbEpisode : tvdbEpisodes) {
      Integer tvdbSeason = tvdbEpisode.seasonNumber.getValue();
      Integer tvdbEpisodeNumber = tvdbEpisode.episodeNumber.getValue();

      if (tvdbSeason == null || tvdbEpisodeNumber == null) {
        return null;
      }

      if (tvdbSeason.equals(seasonNumber) && tvdbEpisodeNumber.equals(episodeNumber)) {
        return tvdbEpisode;
      }
    }
    return null;
  }


}
