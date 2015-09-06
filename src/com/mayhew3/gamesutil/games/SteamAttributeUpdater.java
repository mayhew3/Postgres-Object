package com.mayhew3.gamesutil.games;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.mediaobject.Game;
import com.mayhew3.gamesutil.mediaobject.SteamAttribute;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class SteamAttributeUpdater {

  private Game game;
  private PostgresConnection connection;

  public SteamAttributeUpdater(Game game, PostgresConnection connection) {
    this.game = game;
    this.connection = connection;
  }

  public void runUpdater() throws GameFailedException {
    parseSteamPage();
  }

  private void parseSteamPage() throws GameFailedException {
    Integer steamID = game.steamID.getValue();
    String gameTitle = game.title.getValue();

    try {
      String url = "http://store.steampowered.com/app/" + steamID + "/";
      Document document = Jsoup.connect(url)
          .timeout(3000)
          .userAgent("Chrome")
          .get();

      Elements elements = document.select("[id=category_block]");
      Element first = elements.first();

      if (first == null) {
        throw new GameFailedException("Page found, but no element found with 'category_block' id. Url: " + url);
      }

      List<String> attributes = Lists.newArrayList();

      for (Node node : first.select("[class=game_area_details_specs]")) {
        Node link = node.childNode(1).childNode(0);
        attributes.add(link.toString());
      }

      game.steam_cloud.changeValue(false);
      game.steam_controller.changeValue(false);
      game.steam_local_coop.changeValue(false);

      for (String attribute : attributes) {
        SteamAttribute steamAttribute = new SteamAttribute();
        steamAttribute.initializeForInsert();

        steamAttribute.steamID.changeValue(steamID);
        steamAttribute.attribute.changeValue(attribute);
        steamAttribute.commit(connection);

        if (attribute.equalsIgnoreCase("Steam Cloud")) {
          game.steam_cloud.changeValue(true);
        }
        if (attribute.equalsIgnoreCase("Local Co-op")) {
          game.steam_local_coop.changeValue(true);
        }
        if (attribute.equalsIgnoreCase("Full controller support") ||
            attribute.equalsIgnoreCase("Partial Controller Support")) {
          game.steam_controller.changeValue(true);
        }
      }

      game.steam_attributes.changeValue(new Timestamp(new Date().getTime()));
      game.steam_attribute_count.changeValue(attributes.size());

      game.commit(connection);


    } catch (IOException e) {
      throw new GameFailedException("Couldn't find Steam page for Game " + gameTitle + " (" + steamID  + ")");
    }

  }

}
