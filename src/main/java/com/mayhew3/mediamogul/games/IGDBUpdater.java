package com.mayhew3.mediamogul.games;

import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.games.provider.IGDBProvider;
import com.mayhew3.mediamogul.model.games.Game;
import com.mayhew3.mediamogul.model.games.PossibleGameMatch;
import com.mayhew3.mediamogul.xml.JSONReader;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class IGDBUpdater {
  private Game game;

  private SQLConnection connection;
  private IGDBProvider igdbProvider;
  private JSONReader jsonReader;

  IGDBUpdater(@NotNull Game game, SQLConnection connection, IGDBProvider igdbProvider, JSONReader jsonReader) {
    this.game = game;
    this.connection = connection;
    this.igdbProvider = igdbProvider;
    this.jsonReader = jsonReader;
  }

  void updateGame() {
    igdbProvider.findGameMatches(game.title.getValue(), results -> {
      try {
        processPossibleMatches(results);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void processPossibleMatches(JSONArray results) throws SQLException {
    Optional<JSONObject> exactMatchWithYear = findExactMatchWithYear(results);
    if (exactMatchWithYear.isPresent()) {
      saveExactMatch(exactMatchWithYear.get());
    } else {
      Optional<JSONObject> exactTitleMatch = findExactTitleMatch(results);
      if (exactTitleMatch.isPresent()) {
        saveExactMatch(exactTitleMatch.get());
      } else {
        savePossibleMatches(results);

      }
    }
  }

  private void savePossibleMatches(JSONArray results) throws SQLException {
    jsonReader.forEach(results, possibleMatch -> {
      try {
        savePossibleMatch(possibleMatch);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });

    game.igdb_failed.changeValue(new Date());
    game.commit(connection);
  }

  private void saveExactMatch(JSONObject exactMatch) throws SQLException {
    @NotNull Integer id = jsonReader.getIntegerWithKey(exactMatch, "id");
    @NotNull JSONObject cover = jsonReader.getObjectWithKey(exactMatch, "cover");
    @NotNull String cloudinary_id = jsonReader.getStringWithKey(cover, "cloudinary_id");
    @NotNull Integer width = jsonReader.getIntegerWithKey(cover, "width");
    @NotNull Integer height = jsonReader.getIntegerWithKey(cover, "height");

    game.igdb_id.changeValue(id);
    game.igdb_success.changeValue(new Date());
    game.igdb_poster.changeValue(cloudinary_id);
    game.igdb_poster_w.changeValue(width);
    game.igdb_poster_h.changeValue(height);

    game.commit(connection);
  }

  private void savePossibleMatch(JSONObject possibleMatch) throws SQLException {
    @NotNull Integer id = jsonReader.getIntegerWithKey(possibleMatch, "id");
    @NotNull String name = jsonReader.getStringWithKey(possibleMatch, "name");
    @NotNull JSONObject cover = jsonReader.getObjectWithKey(possibleMatch, "cover");
    @NotNull String cloudinary_id = jsonReader.getStringWithKey(cover, "cloudinary_id");
    @NotNull Integer width = jsonReader.getIntegerWithKey(cover, "width");
    @NotNull Integer height = jsonReader.getIntegerWithKey(cover, "height");

    PossibleGameMatch possibleGameMatch = new PossibleGameMatch();
    possibleGameMatch.initializeForInsert();

    possibleGameMatch.gameId.changeValue(game.id.getValue());
    possibleGameMatch.igdbGameExtId.changeValue(id);
    possibleGameMatch.igdbGameTitle.changeValue(name);
    possibleGameMatch.poster.changeValue(cloudinary_id);
    possibleGameMatch.poster_w.changeValue(width);
    possibleGameMatch.poster_h.changeValue(height);

    possibleGameMatch.commit(connection);
  }

  private Optional<JSONObject> findExactTitleMatch(JSONArray possibleMatches) {
    String searchString = game.title.getValue();

    return findExactMatchForString(possibleMatches, searchString);
  }

  private Optional<JSONObject> findExactMatchWithYear(JSONArray possibleMatches) {
    String searchString = game.title.getValue() + " (" + game.year.getValue() + ")";

    return findExactMatchForString(possibleMatches, searchString);
  }

  private Optional<JSONObject> findExactMatchForString(JSONArray possibleMatches, String searchString) {
    List<JSONObject> matches = jsonReader.findMatches(possibleMatches, (possibleMatch) -> {
      String name = jsonReader.getStringWithKey(possibleMatch, "name");
      return searchString.equalsIgnoreCase(name);
    });
    if (matches.size() == 1) {
      return Optional.of(matches.get(0));
    } else {
      return Optional.empty();
    }
  }

  protected static void debug(Object object) {
    System.out.println(object);
  }


}
