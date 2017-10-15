package com.mayhew3.mediamogul.tv;

import com.mayhew3.mediamogul.ArgumentChecker;
import com.mayhew3.mediamogul.scheduler.UpdateRunner;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.model.tv.Episode;
import com.mayhew3.mediamogul.model.tv.EpisodeGroupRating;
import com.mayhew3.mediamogul.model.tv.EpisodeRating;
import com.mayhew3.mediamogul.model.tv.Series;
import com.mayhew3.mediamogul.tv.helper.UpdateMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"OptionalIsPresent"})
public class EpisodeGroupUpdater implements UpdateRunner {

  private SQLConnection connection;

  @SuppressWarnings("WeakerAccess")
  static Integer currentYear = 2017;

  public static void main(String... args) throws URISyntaxException, SQLException {
    ArgumentChecker argumentChecker = new ArgumentChecker(args);
    SQLConnection connection = PostgresConnectionFactory.createConnection(argumentChecker);

    EpisodeGroupUpdater updater = new EpisodeGroupUpdater(connection);
    updater.runUpdate();
  }

  public EpisodeGroupUpdater(SQLConnection connection) {
    this.connection = connection;
  }

  @SuppressWarnings("SameParameterValue")
  public void runUpdate() throws SQLException {
    String sql = "select s.*\n" +
        "from episode e\n" +
        "inner join series s\n" +
        " on e.series_id = s.id\n" +
        "where e.air_date between ? and ?\n" +
        "and e.watched = ?\n" +
        "and e.retired = ?\n" +
        "group by s.id";

//    DateTime dateTime = new DateTime(2016, 1, 1, 0, 0, 0);

//    String startDate = year + "-01-01";
//    String endDate =  year + "-12-31";
    Timestamp startDate = new Timestamp(beginningOfYear(currentYear).toDate().getTime());
    Timestamp endDate = new Timestamp(endOfYear(currentYear).toDate().getTime());

    Timestamp lastWatchDate = new Timestamp(new DateTime(2017, 1, 11, 0, 0, 0).toDate().getTime());

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, startDate, endDate, true, 0);

    while (resultSet.next()) {
      Series series = new Series();
      series.initializeFromDBObject(resultSet);

      debug("Updating Series '" + series.seriesTitle.getValue() + "' (" + series.id.getValue() + ")");

      EpisodeGroupRating groupRating = getOrCreateExistingRatingForSeriesAndYear(series, currentYear);

      List<EpisodeInfo> episodeInfos = getEligibleEpisodeInfos(groupRating);

      groupRating.numEpisodes.changeValue(episodeInfos.size());
      groupRating.watched.changeValue(getNumberOfWatchedEpisodes(episodeInfos));
      groupRating.rated.changeValue(getNumberOfRatedEpisodes(episodeInfos));
      groupRating.aired.changeValue(getNumberOfAiredEpisodes(episodeInfos));

      groupRating.lastAired.changeValue(getLastAired(episodeInfos));

      groupRating.avgRating.changeValue(getAvgRating(episodeInfos));
      groupRating.maxRating.changeValue(getMaxRating(episodeInfos));
      groupRating.lastRating.changeValue(getLastRating(episodeInfos));

      groupRating.avgFunny.changeValue(getAvgFunny(episodeInfos));
      groupRating.avgCharacter.changeValue(getAvgCharacter(episodeInfos));
      groupRating.avgStory.changeValue(getAvgStory(episodeInfos));

      groupRating.suggestedRating.changeValue(getSuggestedRating(groupRating));

      debug(" - " + groupRating.rated.getValue() + "/" + groupRating.numEpisodes.getValue() + " ratings found, AVG: " + groupRating.avgRating.getValue());

      groupRating.commit(connection);
    }

  }

  protected void debug(Object object) {
    System.out.println(object);
  }

  private Integer getNumberOfRatedEpisodes(List<EpisodeInfo> episodeInfos) {
    return episodeInfos.stream()
        .filter(episodeInfo -> episodeInfo.episodeRating != null)
        .collect(Collectors.toList())
        .size();
  }

  private Integer getNumberOfWatchedEpisodes(List<EpisodeInfo> episodeInfos) {
    return episodeInfos.stream()
        .filter(episodeInfo -> episodeInfo.episode.watched.getValue())
        .collect(Collectors.toList())
        .size();
  }

  private Integer getNumberOfAiredEpisodes(List<EpisodeInfo> episodeInfos) {
    Timestamp now = new Timestamp(new Date().getTime());
    return episodeInfos.stream()
        .filter(episodeInfo -> episodeInfo.episode.airDate.getValue().before(now))
        .collect(Collectors.toList())
        .size();
  }


  @Nullable
  private Timestamp getLastAired(List<EpisodeInfo> episodeInfos) {
    Comparator<EpisodeInfo> byAirDate = Comparator.comparing(a -> a.episode.airDate.getValue());
    Optional<Timestamp> first = episodeInfos.stream()
        .sorted(byAirDate.reversed())
        .map(episodeInfo -> episodeInfo.episode.airDate.getValue())
        .findFirst();
    return first.isPresent() ? first.get() : null;
  }

  private List<EpisodeInfo> getEligibleEpisodeInfos(EpisodeGroupRating groupRating) throws SQLException {
    String sql = "select *\n" +
        "from episode\n" +
        "where air_date between ? and ?\n" +
        "and series_id = ?\n" +
        "and season <> ? \n" +
        "and retired = ?\n" +
        "order by air_date";

    List<Episode> episodes = new ArrayList<>();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, groupRating.startDate.getValue(), groupRating.endDate.getValue(), groupRating.seriesId.getValue(), 0, 0);

    while (resultSet.next()) {
      Episode episode = new Episode();
      episode.initializeFromDBObject(resultSet);

      episodes.add(episode);
    }

    return populateInfos(episodes);
  }

  private List<EpisodeInfo> populateInfos(List<Episode> episodes) throws SQLException {
    List<EpisodeInfo> infos = new ArrayList<>();
    for (Episode episode : episodes) {
      infos.add(new EpisodeInfo(episode));
    }
    return infos;
  }

  @Override
  public String getRunnerName() {
    return "Episode Group Updater";
  }

  @Override
  public @Nullable UpdateMode getUpdateMode() {
    return null;
  }

  private class EpisodeInfo {
    Episode episode;
    @Nullable EpisodeRating episodeRating;

    EpisodeInfo(Episode episode) throws SQLException {
      this.episode = episode;
      this.episodeRating = episode.getMostRecentRating(connection, Optional.empty());
    }

  }

  @SuppressWarnings("ConstantConditions")
  @Nullable
  private BigDecimal getAvgRating(List<EpisodeInfo> episodeInfos) throws SQLException {
    List<BigDecimal> ratings = episodeInfos.stream()
        .map(episodeInfo -> episodeInfo.episodeRating)
        .filter(Objects::nonNull)
        .map(episodeRating -> episodeRating.ratingValue.getValue())
        .collect(Collectors.toList());

    if (ratings.isEmpty()) {
      return null;
    }

    BigDecimal totalRating = ratings.stream()
        .reduce(BigDecimal::add)
        .get();
    return totalRating.divide(BigDecimal.valueOf(ratings.size()), 1, BigDecimal.ROUND_HALF_UP);
  }

  @SuppressWarnings("ConstantConditions")
  @Nullable
  private BigDecimal getAvgFunny(List<EpisodeInfo> episodeInfos) throws SQLException {
    List<BigDecimal> ratings = episodeInfos.stream()
        .map(episodeInfo -> episodeInfo.episodeRating)
        .filter(Objects::nonNull)
        .map(episodeRating -> episodeRating.ratingFunny.getValue())
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    if (ratings.isEmpty()) {
      return null;
    }

    BigDecimal totalRating = ratings.stream()
        .reduce(BigDecimal::add)
        .get();
    return totalRating.divide(BigDecimal.valueOf(ratings.size()), 1, BigDecimal.ROUND_HALF_UP);
  }

  @SuppressWarnings("ConstantConditions")
  @Nullable
  private BigDecimal getAvgCharacter(List<EpisodeInfo> episodeInfos) throws SQLException {
    List<BigDecimal> ratings = episodeInfos.stream()
        .map(episodeInfo -> episodeInfo.episodeRating)
        .filter(Objects::nonNull)
        .map(episodeRating -> episodeRating.ratingCharacter.getValue())
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    if (ratings.isEmpty()) {
      return null;
    }

    BigDecimal totalRating = ratings.stream()
        .reduce(BigDecimal::add)
        .get();
    return totalRating.divide(BigDecimal.valueOf(ratings.size()), 1, BigDecimal.ROUND_HALF_UP);
  }

  @SuppressWarnings("ConstantConditions")
  @Nullable
  private BigDecimal getAvgStory(List<EpisodeInfo> episodeInfos) throws SQLException {
    List<BigDecimal> ratings = episodeInfos.stream()
        .map(episodeInfo -> episodeInfo.episodeRating)
        .filter(Objects::nonNull)
        .map(episodeRating -> episodeRating.ratingStory.getValue())
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    if (ratings.isEmpty()) {
      return null;
    }

    BigDecimal totalRating = ratings.stream()
        .reduce(BigDecimal::add)
        .get();
    return totalRating.divide(BigDecimal.valueOf(ratings.size()), 1, BigDecimal.ROUND_HALF_UP);
  }

  @Nullable
  private BigDecimal getMaxRating(List<EpisodeInfo> episodeInfos) throws SQLException {
    Optional<BigDecimal> max = episodeInfos.stream()
        .map(episodeInfo -> episodeInfo.episodeRating)
        .filter(Objects::nonNull)
        .map(episodeRating -> episodeRating.ratingValue.getValue())
        .reduce(BigDecimal::max);
    return max.isPresent() ? max.get() : null;
  }

  @Nullable
  private BigDecimal getLastRating(List<EpisodeInfo> episodeInfos) throws SQLException {
    Comparator<EpisodeInfo> byAirDate = Comparator.comparing(a -> a.episode.airDate.getValue());
    Comparator<EpisodeInfo> byEpisodeNumber = Comparator.comparing(a -> a.episode.episodeNumber.getValue());
    Optional<EpisodeRating> lastRating = episodeInfos.stream()
        .sorted(byAirDate.thenComparing(byEpisodeNumber).reversed())
        .map(episodeInfo -> episodeInfo.episodeRating)
        .filter(Objects::nonNull)
        .findFirst();
    return lastRating.isPresent() ? lastRating.get().ratingValue.getValue() : null;
  }

  @Nullable
  private BigDecimal getSuggestedRating(EpisodeGroupRating groupRating) throws SQLException {
    BigDecimal average = groupRating.avgRating.getValue();
    BigDecimal max = groupRating.maxRating.getValue();
    BigDecimal last = groupRating.lastRating.getValue();

    if (average == null || max == null || last == null) {
      return null;
    }

    BigDecimal total = average.multiply(BigDecimal.valueOf(5))
        .add(max.multiply(BigDecimal.valueOf(3)))
        .add(last);

    return total.divide(BigDecimal.valueOf(9), BigDecimal.ROUND_HALF_UP);
  }

  @NotNull
  private EpisodeGroupRating getOrCreateExistingRatingForSeriesAndYear(Series series, Integer year) throws SQLException {
    Optional<EpisodeGroupRating> existingRating = findRatingForSeriesAndYear(series, year);

    EpisodeGroupRating groupRating = new EpisodeGroupRating();
    if (existingRating.isPresent()) {
      return existingRating.get();
    } else {
      Integer previousYear = year - 1;
      Optional<EpisodeGroupRating> previousYearRating = findRatingForSeriesAndYear(series, previousYear);

      DateTime startDate = previousYearRating.isPresent() ?
          nextDay(previousYearRating.get().endDate.getValue()) :
          beginningOfYear(year);

      DateTime endDate = endOfYear(year);

      groupRating.initializeForInsert();
      groupRating.seriesId.changeValue(series.id.getValue());
      groupRating.year.changeValue(year);
      groupRating.startDate.changeValue(new Timestamp(startDate.toDate().getTime()));
      groupRating.endDate.changeValue(new Timestamp(endDate.toDate().getTime()));
    }
    return groupRating;
  }

  private DateTime nextDay(Timestamp day) {
    return new DateTime(day).plusDays(1);
  }

  @NotNull
  private DateTime endOfYear(Integer year) {
    return new DateTime(year, 12, 31, 0, 0, 0);
  }

  @NotNull
  private DateTime beginningOfYear(Integer year) {
    return new DateTime(year, 1, 1, 0, 0, 0);
  }

  private Optional<EpisodeGroupRating> findRatingForSeriesAndYear(Series series, Integer year) throws SQLException {
    String sql = "select *\n" +
        "from episode_group_rating\n" +
        "where series_id = ?\n" +
        "and year = ?";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, series.id.getValue(), year);

    EpisodeGroupRating groupRating = new EpisodeGroupRating();
    if (resultSet.next()) {
      groupRating.initializeFromDBObject(resultSet);
      return Optional.of(groupRating);
    } else {
      return Optional.empty();
    }
  }

}
