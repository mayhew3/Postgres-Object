package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.dataobject.MetacriticSeason;
import com.mayhew3.gamesutil.dataobject.SeriesPostgres;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mongodb.BasicDBObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MetacriticTVUpdater {

  private SeriesPostgres series;
  private SQLConnection connection;

  public MetacriticTVUpdater(SeriesPostgres series, SQLConnection connection) {
    this.series = series;
    this.connection = connection;
  }

  public void runUpdater() throws ShowFailedException {
    parseMetacritic();
  }

  private void parseMetacritic() throws ShowFailedException {
    String title = series.seriesTitle.getValue();
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
      findMetacriticForString(formattedTitle, 1);
    } catch (IOException e) {
      throw new ShowFailedException("Couldn't find Metacritic page for series '" + title + "' with formatted '" + formattedTitle + "'");
    } catch (SQLException e) {
      throw new ShowFailedException("Error updating DB for series '" + title + "' with formatted '" + formattedTitle + "'");
    }

    while (!failed) {
      seasonNumber++;

      try {
        findMetacriticForString(formattedTitle + "/season-" + seasonNumber, seasonNumber);
      } catch (Exception e) {
        failed = true;
        debug("Finished finding seasons after Season " + (seasonNumber-1));
      }
    }

  }

  private void findMetacriticForString(String formattedTitle, Integer season) throws IOException, ShowFailedException, SQLException {
    Document document = Jsoup.connect("http://www.metacritic.com/tv/" + formattedTitle)
        .timeout(3000)
        .userAgent("Mozilla")
        .get();

    if (season > 1) {
      Elements select = document.select("[href=/tv/" + formattedTitle + "]");
      if (select.isEmpty()) {
        throw new ShowFailedException("Current season doesn't exist.");
      }
    }

    Elements elements = document.select("[itemprop=ratingValue]");
    Element first = elements.first();

    if (first == null) {
      throw new ShowFailedException("Page found, but no element found with 'ratingValue' id.");
    }

    Node metacriticValue = first.childNodes().get(0);

    Integer metaCritic = Integer.valueOf(metacriticValue.toString());

    String sql = "SELECT * " +
        "FROM metacritic_season " +
        "WHERE series_id = ? " +
        "AND season = ?";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, series.id.getValue(), season);

    MetacriticSeason metacriticSeason = new MetacriticSeason();

    if (resultSet.next()) {
      metacriticSeason.initializeFromDBObject(resultSet);
    } else {
      metacriticSeason.initializeForInsert();
    }

    debug("Updating Season " + season + " (" + metaCritic + ")");

    series.metacritic.changeValue(metaCritic);
    series.commit(connection);
  }

  protected void debug(Object object) {
    System.out.println(object);
  }

}
