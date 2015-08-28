package com.mayhew3.gamesutil.games;

import com.google.common.collect.Maps;
import com.mayhew3.gamesutil.mediaobject.Game;
import com.mayhew3.gamesutil.mediaobject.GameLog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

public class MetacriticGameUpdater {

  private Game game;
  private PostgresConnection connection;

  public MetacriticGameUpdater(Game game, PostgresConnection connection) {
    this.game = game;
    this.connection = connection;
  }

  public void runUpdater() throws GameFailedException {
    parseMetacritic();
  }

  private void parseMetacritic() throws GameFailedException {
    String title = game.game.getValue();
    String hint = game.metacriticHint.getValue();
    String platform = game.platform.getValue();
    String formattedTitle = hint == null ?
        title
        .toLowerCase()
        .replaceAll(" ", "-")
        .replaceAll("'", "")
        .replaceAll("\\.", "") :
        hint
        ;

    try {
      Map<String, String> formattedPlatforms = Maps.newHashMap();
      formattedPlatforms.put("PC", "pc");
      formattedPlatforms.put("Steam", "pc");
      formattedPlatforms.put("Xbox 360", "xbox-360");
      formattedPlatforms.put("PS3", "playstation-3");
      formattedPlatforms.put("Wii", "wii");
      formattedPlatforms.put("Wii U", "wii-u");
      formattedPlatforms.put("DS", "ds");
      formattedPlatforms.put("Xbox", "xbox");

      String formattedPlatform = formattedPlatforms.get(platform);

      Document document = Jsoup.connect("http://www.metacritic.com/game/" + formattedPlatform + "/" + formattedTitle)
          .timeout(3000)
          .userAgent("Mozilla")
          .get();

      game.metacriticPage.changeValue(true);

      Elements elements = document.select("[itemprop=ratingValue]");
      Element first = elements.first();

      if (first == null) {
        game.commit(connection);
        throw new GameFailedException("Page found, but no element found with 'ratingValue' id.");
      }

      Node metacriticValue = first.childNodes().get(0);

      Integer metaCritic = Integer.valueOf(metacriticValue.toString());

      game.metacriticMatched.changeValue(new Timestamp(new Date().getTime()));

      BigDecimal previousValue = game.metacritic.getValue();
      BigDecimal updatedValue = new BigDecimal(metaCritic);

      game.metacritic.changeValue(updatedValue);


      if (previousValue == null || previousValue.compareTo(updatedValue) != 0) {
        GameLog gameLog = new GameLog();
        gameLog.initializeForInsert();

        gameLog.game.changeValue(title);
        gameLog.steamID.changeValue(game.steamID.getValue());
        gameLog.platform.changeValue(platform);
        gameLog.previousPlaytime.changeValue(previousValue);
        gameLog.updatedplaytime.changeValue(updatedValue);

        if (previousValue != null) {
          gameLog.diff.changeValue(updatedValue.subtract(previousValue));
        }

        gameLog.eventtype.changeValue("Metacritic");
        gameLog.eventdate.changeValue(new Timestamp(new Date().getTime()));

        gameLog.commit(connection);
      }

      game.commit(connection);

    } catch (IOException e) {
      throw new GameFailedException("Couldn't find Metacritic page for series '" + title + "' with formatted '" + formattedTitle + "'");
    }

  }

}
