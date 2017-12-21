package com.mayhew3.mediamogul.tv.blog;

import com.google.common.collect.Maps;
import com.mayhew3.mediamogul.ArgumentChecker;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.model.tv.Episode;
import com.mayhew3.mediamogul.model.tv.EpisodeGroupRating;
import com.mayhew3.mediamogul.model.tv.EpisodeRating;
import com.mayhew3.mediamogul.model.tv.Series;
import com.mayhew3.mediamogul.tv.EpisodeGroupUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BlogRankingsCreator {

  private SQLConnection connection;
  private Path templateFile;
  private String outputPath;

  private BlogRankingsCreator(SQLConnection connection, String templatePath, String outputPath) {
    this.connection = connection;
    templateFile = Paths.get(templatePath);
    this.outputPath = outputPath;
  }

  public static void main(String... args) throws URISyntaxException, SQLException, IOException {
    ArgumentChecker argumentChecker = new ArgumentChecker(args);
    Optional<String> templateFilePath = argumentChecker.getTemplateFilePath();
    if (!templateFilePath.isPresent()) {
      throw new IllegalStateException("No 'template' argument provided.");
    }

    Optional<String> blogOutputFilePath = argumentChecker.getBlogOutputFilePath();
    if (!blogOutputFilePath.isPresent()) {
      throw new IllegalStateException("No 'blog' argument provided.");
    }

    SQLConnection connection = PostgresConnectionFactory.createConnection(argumentChecker);

    BlogRankingsCreator blogRankingsCreator = new BlogRankingsCreator(connection, templateFilePath.get(), blogOutputFilePath.get());
    blogRankingsCreator.execute();
  }

  private void execute() throws IOException, SQLException {
    File outputFile = new File(outputPath + "/blog_output1.html");
    FileOutputStream outputStream = new FileOutputStream(outputFile, false);

    String contents = new String(Files.readAllBytes(templateFile));


    String combinedExport = fetchSeriesAndCombineWithTemplate(contents);


    outputStream.write(combinedExport.getBytes());
    outputStream.close();
  }

  private String fetchSeriesAndCombineWithTemplate(String templateContents) throws SQLException {

    StringBuilder export = new StringBuilder();

    BlogTemplatePrinter blogTemplatePrinter = new BlogTemplatePrinter(templateContents);

    String reusableJoins = "FROM episode_group_rating " +
        "WHERE year = ? " +
        "AND retired = ? " +
        "AND aired > ? " +
        "AND aired = watched ";

    Integer totalShows = getSeriesCount(reusableJoins);

    String fullSql = "SELECT * " +
        reusableJoins +
        "ORDER BY coalesce(rating, suggested_rating)  ASC ";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(fullSql, 2017, 0, 0);

    Integer currentRanking = totalShows;

    while (resultSet.next()) {
      EpisodeGroupRating episodeGroupRating = new EpisodeGroupRating();
      episodeGroupRating.initializeFromDBObject(resultSet);

      export.append(getExportForSeries(blogTemplatePrinter, episodeGroupRating, currentRanking));
      export.append("<br>");

      currentRanking--;
    }

    return export.toString();
  }

  @NotNull
  private Integer getSeriesCount(String reusableJoins) throws SQLException {
    String countSql = "SELECT COUNT(1) as series_count " +
        reusableJoins;

    ResultSet resultSet1 = connection.prepareAndExecuteStatementFetch(countSql, 2017, 0, 0);

    Integer totalShows = 0;
    if (resultSet1.next()) {
      totalShows = resultSet1.getInt("series_count");
    }
    return totalShows;
  }

  private String getExportForSeries(BlogTemplatePrinter blogTemplatePrinter, EpisodeGroupRating episodeGroupRating, Integer currentRanking) throws SQLException {
    blogTemplatePrinter.clearMappings();

    Series series = getSeries(episodeGroupRating);

    BigDecimal effectiveRating = episodeGroupRating.rating.getValue() == null ?
        episodeGroupRating.suggestedRating.getValue() :
        episodeGroupRating.rating.getValue();

    List<EpisodeInfo> episodeInfos = getEligibleEpisodeInfos(episodeGroupRating);

    EpisodeInfo bestEpisode = getBestEpisode(episodeGroupRating, episodeInfos);
    BigDecimal bestEpisodeRating = bestEpisode.episodeRating.ratingValue.getValue();

    blogTemplatePrinter.addMapping("POSTER_FILENAME", series.poster.getValue());
    blogTemplatePrinter.addMapping("RANKING_VALUE", Integer.toString(currentRanking));
    blogTemplatePrinter.addMapping("RATING_COLOR", getHSLAMethod(effectiveRating));
    blogTemplatePrinter.addMapping("RATING_VALUE", effectiveRating.toString());
    blogTemplatePrinter.addMapping("SERIES_NAME", series.seriesTitle.getValue());
    blogTemplatePrinter.addMapping("SEASONS_TEXT", "Seasons 9/10");
    blogTemplatePrinter.addMapping("EPISODE_COUNT", Integer.toString(episodeGroupRating.aired.getValue()));
    blogTemplatePrinter.addMapping("FEATURED_RATING_COLOR", getHSLAMethod(bestEpisodeRating));
    blogTemplatePrinter.addMapping("FEATURED_RATING_VALUE", bestEpisodeRating.toString());
    blogTemplatePrinter.addMapping("FEATURED_EPISODE_NUMBER", bestEpisode.episode.getSeason() + "x" + bestEpisode.episode.episodeNumber.getValue());
    blogTemplatePrinter.addMapping("FEATURED_EPISODE_TITLE", bestEpisode.episode.title.getValue());
    blogTemplatePrinter.addMapping("REVIEW_TEXT", episodeGroupRating.review.getValue());

    return blogTemplatePrinter.createCombinedExport();
  }

  @NotNull
  private BlogRankingsCreator.EpisodeInfo getBestEpisode(EpisodeGroupRating episodeGroupRating, List<EpisodeInfo> episodeInfos) {
    Optional<EpisodeInfo> first = episodeInfos.stream()
        .filter(episodeInfo -> episodeInfo.episodeRating != null &&
            episodeGroupRating.maxRating.getValue().equals(episodeInfo.episodeRating.ratingValue.getValue()))
        .findFirst();

    if (!first.isPresent()) {
      throw new IllegalStateException("No episode found with max rating!");
    }

    return first.get();
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

  private class EpisodeInfo {
    Episode episode;
    @Nullable EpisodeRating episodeRating;

    EpisodeInfo(Episode episode) throws SQLException {
      this.episode = episode;
      this.episodeRating = episode.getMostRecentRating(connection, Optional.empty());
    }

  }

  private Series getSeries(EpisodeGroupRating episodeGroupRating) throws SQLException {
    String sql = "SELECT * " +
        "FROM series " +
        "WHERE id = ? ";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, episodeGroupRating.seriesId.getValue());
    if (resultSet.next()) {
      Series series = new Series();
      series.initializeFromDBObject(resultSet);

      return series;
    } else {
      throw new IllegalStateException("No series found with id: " + episodeGroupRating.seriesId.getValue() + ", linked to EpisodeGroupRating id: " + episodeGroupRating.id.getValue());
    }
  }

  private String getHSLAMethod(BigDecimal value) {
    BigDecimal hue = getHue(value);
    String saturation = (value == null) ? "0%" : "50%";
    return "hsla(" + hue + ", " + saturation + ", 42%, 1)";
  }

  private BigDecimal getHue(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO;
    }

    BigDecimal fifty = BigDecimal.valueOf(50);
    BigDecimal half = BigDecimal.valueOf(.5);

    // matches javascript code from seriesDetailController
    return
        (value.compareTo(fifty) <= 0) ?
        value.multiply(half) :
        (fifty.multiply(half).add((value.subtract(fifty).multiply(BigDecimal.valueOf(4.5)))));
  }
}
