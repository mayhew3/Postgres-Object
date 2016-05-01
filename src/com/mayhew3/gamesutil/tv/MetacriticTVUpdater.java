package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.model.tv.Season;
import com.mayhew3.gamesutil.model.tv.Series;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class MetacriticTVUpdater {

  private SQLConnection connection;

  public MetacriticTVUpdater(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String... args) throws URISyntaxException, SQLException, MetacriticException {
    List<String> argList = Lists.newArrayList(args);
    Boolean singleSeries = argList.contains("SingleSeries");
    String identifier = new ArgumentChecker(args).getDBIdentifier();

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);
    MetacriticTVUpdater metacriticTVUpdater = new MetacriticTVUpdater(connection);

    if (singleSeries) {
      metacriticTVUpdater.runUpdateSingle();
    } else {
      metacriticTVUpdater.runUpdater();
    }
  }

  public void runUpdater() throws SQLException {
    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false);

    runUpdateOnResultSet(resultSet);
  }

  private void runUpdateSingle() throws SQLException {
    String singleSeriesTitle = "Idiotsitter"; // update for testing on a single series

    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ? " +
        "and title = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, singleSeriesTitle);

    runUpdateOnResultSet(resultSet);
  }

  private void runUpdateOnResultSet(ResultSet resultSet) throws SQLException {
    debug("Starting update.");

    int i = 0;

    while (resultSet.next()) {
      i++;
      Series series = new Series();
      series.initializeFromDBObject(resultSet);

      try {
        parseMetacritic(series);
      } catch (MetacriticException e) {
        debug("Unable to find metacritic for: " + series.seriesTitle.getValue());
      } catch (Exception e) {
        e.printStackTrace();
        debug("Uncaught exception during metacritic fetch: " + series.seriesTitle.getValue());
      }

      debug(i + " processed.");
    }
  }

  private void parseMetacritic(Series series) throws MetacriticException, SQLException {
    String title = series.seriesTitle.getValue();
    debug("Metacritic update for: " + title);

    String hint = series.metacriticHint.getValue();
    String formattedTitle = hint == null ?
        title
        .toLowerCase()
        .replaceAll(" ", "-")
        .replaceAll("'", "")
        .replaceAll("\\.", "") :
        hint
        ;

    Integer seasonNumber = 1;
    Boolean failed = false;

    try {
      findMetacriticForString(series, formattedTitle, 1);
    } catch (IOException e) {
      throw new MetacriticException("Couldn't find Metacritic page for series '" + title + "' with formatted '" + formattedTitle + "'");
    }

    while (!failed) {
      seasonNumber++;

      try {
        findMetacriticForString(series, formattedTitle + "/season-" + seasonNumber, seasonNumber);
      } catch (Exception e) {
        failed = true;
        debug("Finished finding seasons after Season " + (seasonNumber-1));
      }
    }

  }

  private void findMetacriticForString(Series series, String formattedTitle, Integer seasonNumber) throws IOException, SQLException, MetacriticException {
    Document document = Jsoup.connect("http://www.metacritic.com/tv/" + formattedTitle)
        .timeout(3000)
        .userAgent("Mozilla")
        .get();

    if (seasonNumber > 1) {
      Elements select = document.select("[href=/tv/" + formattedTitle + "]");
      if (select.isEmpty()) {
        throw new MetacriticException("Current season doesn't exist.");
      }
    }

    Elements elements = document.select("[itemprop=ratingValue]");
    Element first = elements.first();

    if (first == null) {
      throw new MetacriticException("Page found, but no element found with 'ratingValue' id.");
    }

    Node metacriticValue = first.childNodes().get(0);

    Integer metaCritic = Integer.valueOf(metacriticValue.toString());

    String sql = "SELECT * " +
        "FROM season " +
        "WHERE series_id = ? " +
        "AND season_number = ?";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, series.id.getValue(), seasonNumber);

    Season season = new Season();

    if (resultSet.next()) {
      season.initializeFromDBObject(resultSet);
    } else {
      season.initializeForInsert();
      season.dateModified.changeValue(new Date());
    }

    debug("Updating Season " + seasonNumber + " (" + metaCritic + ")");

    season.metacritic.changeValue(metaCritic);
    season.seasonNumber.changeValue(seasonNumber);
    season.seriesId.changeValue(series.id.getValue());

    if (season.hasChanged()) {
      season.dateModified.changeValue(new Date());
    }

    season.commit(connection);

    series.metacritic.changeValue(metaCritic);
    series.commit(connection);
  }

  protected void debug(Object object) {
    System.out.println(object);
  }

}
