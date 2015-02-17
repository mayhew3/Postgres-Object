package com.mayhew3.gamesutil;

import com.mayhew3.gamesutil.mediaobject.Series;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;

public class MetacriticUpdater extends TVDatabaseUtility {

  private Series _series;


  public MetacriticUpdater(MongoClient client, DB db, Series series) {
    super(client, db);
    _series = series;
  }

  public void runUpdater() {
    try {
      parseMetacritic();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void parseMetacritic() throws IOException {
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

    Document document = Jsoup.connect("http://www.metacritic.com/tv/" + formattedTitle)
        .timeout(3000)
        .userAgent("Mozilla")
        .get();

    Elements elements = document.select("[itemprop=ratingValue]");
    Node metacriticValue = elements.first().childNodes().get(0);

    Integer metaCritic = Integer.valueOf(metacriticValue.toString());

    _series.metacritic.changeValue(metaCritic);
    _series.commit(_db);
  }
}
