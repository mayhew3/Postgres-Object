package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.mediaobject.Series;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;

public class MetacriticTVUpdater extends TVDatabaseUtility {

  private Series _series;


  public MetacriticTVUpdater(MongoClient client, DB db, Series series) {
    super(client, db);
    _series = series;
  }

  public void runUpdater() throws ShowFailedException {
    parseMetacritic();
  }

  private void parseMetacritic() throws ShowFailedException {
    String title = _series.seriesTitle.getValue();
    String hint = _series.metacriticHint.getValue();
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

  private void findMetacriticForString(String formattedTitle, Integer season) throws IOException, ShowFailedException {
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

    BasicDBObject metaSeason = new BasicDBObject()
        .append("SeasonNumber", season)
        .append("SeasonMetacritic", metaCritic);

    debug("Updating Season " + season + " (" + metaCritic + ")");

    _series.metacriticSeasons.addToArray(metaSeason, "SeasonNumber");
    _series.metacritic.changeValue(metaCritic);
    _series.commit(_db);
  }
}
