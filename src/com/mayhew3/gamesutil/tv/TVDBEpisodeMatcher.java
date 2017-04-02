package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.model.tv.PossibleEpisodeMatch;
import com.mayhew3.gamesutil.model.tv.TVDBEpisode;
import com.mayhew3.gamesutil.model.tv.TiVoEpisode;
import com.mayhew3.gamesutil.db.SQLConnection;
import info.debatty.java.stringsimilarity.NGram;
import info.debatty.java.stringsimilarity.interfaces.StringDistance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

class TVDBEpisodeMatcher {
  private SQLConnection connection;
  private TiVoEpisode tiVoEpisode;
  private Integer seriesId;

  private List<TVDBEpisode> allTVDBEpisodes = new ArrayList<>();

  private StringDistance distanceCalculator = new NGram();

  TVDBEpisodeMatcher(SQLConnection connection, TiVoEpisode tiVoEpisode, Integer seriesId) {
    this.connection = connection;
    this.tiVoEpisode = tiVoEpisode;
    this.seriesId = seriesId;
  }

  @Nullable TVDBEpisode findTVDBEpisodeMatch() throws SQLException {
    String episodeTitle = tiVoEpisode.title.getValue();
    Integer episodeNumber = tiVoEpisode.episodeNumber.getValue();
    Date startTime = tiVoEpisode.showingStartTime.getValue();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT te.* " +
            "FROM episode e " +
            "INNER JOIN tvdb_episode te " +
            "  ON e.tvdb_episode_id = te.id " +
            "WHERE e.series_id = ? " +
            "AND te.retired = ? " +
            "AND e.retired = ? ",
        seriesId, 0, 0
    );

    while(resultSet.next()) {
      TVDBEpisode tvdbEpisode = new TVDBEpisode();
      tvdbEpisode.initializeFromDBObject(resultSet);
      allTVDBEpisodes.add(tvdbEpisode);
    }

    if (episodeTitle != null) {
      for (TVDBEpisode tvdbEpisode : allTVDBEpisodes) {
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
        TVDBEpisode match = checkForNumberMatch(seasonNumber, episodeNumber);
        if (match != null) {
          return match;
        }
      } else {
        String episodeNumberString = episodeNumber.toString();
        int seasonLength = episodeNumberString.length() / 2;

        String seasonString = episodeNumberString.substring(0, seasonLength);
        String episodeString = episodeNumberString.substring(seasonLength, episodeNumberString.length());

        TVDBEpisode match = checkForNumberMatch(Integer.valueOf(seasonString), Integer.valueOf(episodeString));

        if (match != null) {
          return match;
        }
      }
    }

    // no match on episode number. Try air date.

    if (startTime != null) {
      DateTime showingStartTime = new DateTime(startTime);

      for (TVDBEpisode tvdbEpisode : allTVDBEpisodes) {
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

  @Nullable TVDBEpisode findTVDBEpisodeMatchWithPossibleMatches() throws SQLException {
    String episodeTitle = tiVoEpisode.title.getValue();
    Integer episodeNumber = tiVoEpisode.episodeNumber.getValue();
    Date startTime = tiVoEpisode.showingStartTime.getValue();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT te.* " +
            "FROM episode e " +
            "INNER JOIN tvdb_episode te " +
            "  ON e.tvdb_episode_id = te.id " +
            "WHERE e.series_id = ? " +
            "AND te.retired = ? " +
            "AND e.retired = ? ",
        seriesId, 0, 0
    );

    while(resultSet.next()) {
      TVDBEpisode tvdbEpisode = new TVDBEpisode();
      tvdbEpisode.initializeFromDBObject(resultSet);
      allTVDBEpisodes.add(tvdbEpisode);
    }

    List<TVDBEpisode> exactTitleMatches = findExactTitleMatches(episodeTitle);
    List<TVDBEpisode> exactNumberMatches = findExactNumberMatches(episodeNumber);
    List<TVDBEpisode> exactDateMatches = findExactDateMatches(startTime);

    Set<TVDBEpisode> uniqueMatches = collectAllMatches(exactTitleMatches, exactNumberMatches, exactDateMatches);

    if (exactTitleMatches.size() == 1 && uniqueMatches.size() == 1 &&
        (exactNumberMatches.size() == 1 || exactDateMatches.size() == 1)) {
      return exactTitleMatches.get(0);
    } else {
      List<PossibleEpisodeMatch> orderedBestTitleMatches = createOrderedBestTitleMatches(episodeTitle);
    }

    return null;
  }

  private List<PossibleEpisodeMatch> createOrderedBestTitleMatches(String episodeTitle) {
    return allTVDBEpisodes.stream()
        .map(tvdbEpisode -> initializePossibleMatch(tvdbEpisode, episodeTitle))
        .sorted()
        .collect(Collectors.toList());
  }

  private PossibleEpisodeMatch initializePossibleMatch(TVDBEpisode tvdbEpisode, String episodeTitle) {
    Double distance = distanceCalculator.distance(episodeTitle, tvdbEpisode.name.getValue());

    PossibleEpisodeMatch possibleEpisodeMatch = new PossibleEpisodeMatch();
    possibleEpisodeMatch.initializeForInsert();

    possibleEpisodeMatch.seriesId.changeValue(seriesId);
    possibleEpisodeMatch.tivoEpisodeId.changeValue(tiVoEpisode.id.getValue());
    possibleEpisodeMatch.tvdbEpisodeId.changeValue(tvdbEpisode.id.getValue());
    possibleEpisodeMatch.matchScore.changeValue(distance);
    possibleEpisodeMatch.matchAlgorithm.changeValue("NGram");

    return possibleEpisodeMatch;
  }

  @NotNull
  private Set<TVDBEpisode> collectAllMatches(List<TVDBEpisode> exactTitleMatches, List<TVDBEpisode> exactNumberMatches, List<TVDBEpisode> exactDateMatches) {
    Set<TVDBEpisode> uniqueMatches = new HashSet<>();
    uniqueMatches.addAll(exactTitleMatches);
    uniqueMatches.addAll(exactNumberMatches);
    uniqueMatches.addAll(exactDateMatches);
    return uniqueMatches;
  }

  private List<TVDBEpisode> findExactTitleMatches(String episodeTitle) {
    List<TVDBEpisode> matches = new ArrayList<>();

    if (episodeTitle != null) {
      for (TVDBEpisode tvdbEpisode : allTVDBEpisodes) {
        String tvdbTitleObject = tvdbEpisode.name.getValue();

        if (episodeTitle.equalsIgnoreCase(tvdbTitleObject)) {
          matches.add(tvdbEpisode);
        }
      }
    }
    return matches;
  }

  private List<TVDBEpisode> findExactNumberMatches(Integer episodeNumber) {
    List<TVDBEpisode> matches = new ArrayList<>();

    if (episodeNumber != null) {
      Integer seasonNumber = 1;

      if (episodeNumber < 100) {
        TVDBEpisode match = checkForNumberMatch(seasonNumber, episodeNumber);
        if (match != null) {
          matches.add(match);
        }
      } else {
        String episodeNumberString = episodeNumber.toString();
        int seasonLength = episodeNumberString.length() / 2;

        String seasonString = episodeNumberString.substring(0, seasonLength);
        String episodeString = episodeNumberString.substring(seasonLength, episodeNumberString.length());

        TVDBEpisode match = checkForNumberMatch(Integer.valueOf(seasonString), Integer.valueOf(episodeString));

        if (match != null) {
          matches.add(match);
        }
      }
    }

    return matches;
  }

  private List<TVDBEpisode> findExactDateMatches(Date startTime) {
    List<TVDBEpisode> matches = new ArrayList<>();

    if (startTime != null) {
      DateTime showingStartTime = new DateTime(startTime);

      for (TVDBEpisode tvdbEpisode : allTVDBEpisodes) {
        Date firstAiredValue = tvdbEpisode.firstAired.getValue();
        if (firstAiredValue != null) {
          DateTime firstAired = new DateTime(firstAiredValue);

          DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();

          if (comparator.compare(showingStartTime, firstAired) == 0) {
            matches.add(tvdbEpisode);
          }
        }
      }
    }

    return matches;
  }

  @Nullable
  private TVDBEpisode checkForNumberMatch(Integer seasonNumber, Integer episodeNumber) {
    for (TVDBEpisode tvdbEpisode : allTVDBEpisodes) {
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
