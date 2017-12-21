package com.mayhew3.mediamogul.tv.blog;

import com.mayhew3.mediamogul.ArgumentChecker;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.model.tv.EpisodeGroupRating;
import com.mayhew3.mediamogul.model.tv.Series;
import org.jetbrains.annotations.NotNull;

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
import java.util.Optional;

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
        "AND review IS NOT NULL " +
        "AND aired = watched ";

    Integer totalShows = getSeriesCount(reusableJoins);

    String fullSql = "SELECT * " +
        reusableJoins +
        "ORDER BY rating ASC ";

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

    blogTemplatePrinter.addMapping("POSTER_FILENAME", series.poster.getValue());
    blogTemplatePrinter.addMapping("RANKING_VALUE", Integer.toString(currentRanking));
    blogTemplatePrinter.addMapping("RATING_COLOR", "#8da136");
    blogTemplatePrinter.addMapping("RATING_VALUE", episodeGroupRating.rating.getValue().toString());
    blogTemplatePrinter.addMapping("SERIES_NAME", series.seriesTitle.getValue());
    blogTemplatePrinter.addMapping("SEASONS_TEXT", "Seasons 9/10");
    blogTemplatePrinter.addMapping("EPISODE_COUNT", Integer.toString(episodeGroupRating.aired.getValue()));
    blogTemplatePrinter.addMapping("FEATURED_RATING_COLOR", "#36a194");
    blogTemplatePrinter.addMapping("FEATURED_RATING_VALUE", "82");
    blogTemplatePrinter.addMapping("FEATURED_EPISODE_NUMBER", "10x11");
    blogTemplatePrinter.addMapping("FEATURED_EPISODE_TITLE", "She's Got Talent");
    blogTemplatePrinter.addMapping("REVIEW_TEXT", episodeGroupRating.review.getValue());

    return blogTemplatePrinter.createCombinedExport();
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
}
