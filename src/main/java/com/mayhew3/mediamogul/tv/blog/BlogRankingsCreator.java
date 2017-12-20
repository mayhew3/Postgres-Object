package com.mayhew3.mediamogul.tv.blog;

import com.mayhew3.mediamogul.ArgumentChecker;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.model.tv.Series;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    String sql = "SELECT * " +
        "FROM series " +
        "WHERE title = ?";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, "Halt and Catch Fire");
    while (resultSet.next()) {
      Series series = new Series();
      series.initializeFromDBObject(resultSet);

      export.append(getExportForSeries(blogTemplatePrinter, series));
    }

    return export.toString();
  }

  private String getExportForSeries(BlogTemplatePrinter blogTemplatePrinter, Series series) {
    blogTemplatePrinter.clearMappings();

    blogTemplatePrinter.addMapping("POSTER_FILENAME", series.poster.getValue());
    blogTemplatePrinter.addMapping("RANKING_VALUE", "34");
    blogTemplatePrinter.addMapping("RATING_COLOR", "#8da136");
    blogTemplatePrinter.addMapping("RATING_VALUE", "67.9");
    blogTemplatePrinter.addMapping("SERIES_NAME", series.seriesTitle.getValue());
    blogTemplatePrinter.addMapping("SEASONS_TEXT", "Seasons 9/10");
    blogTemplatePrinter.addMapping("EPISODE_COUNT", "12");
    blogTemplatePrinter.addMapping("FEATURED_RATING_COLOR", "#36a194");
    blogTemplatePrinter.addMapping("FEATURED_RATING_VALUE", "82");
    blogTemplatePrinter.addMapping("FEATURED_EPISODE_NUMBER", "10x11");
    blogTemplatePrinter.addMapping("FEATURED_EPISODE_TITLE", "She's Got Talent");
    blogTemplatePrinter.addMapping("REVIEW_TEXT", "See my review for last season, which still mostly applies. It honestly feels like there are these moments, outside of the control of the writers themselves, that for whatever reason, be it momentary inspiration from one of the actors, an especially caffeinated day from the director... but against all odds, these moments just work. And brilliantly. A couple of my biggest laughs this year were to this show. One moment almost brought me to tears. And the other 98% of the content hurt me and made me feel ugly. I'm almost interested in this as a human experiment, to see how much torture I will put myself through for the glimmer of hope for one of those moments. This show isn't good.");

    return blogTemplatePrinter.createCombinedExport();
  }
}
