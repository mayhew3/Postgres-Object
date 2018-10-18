package com.mayhew3.mediamogul.games;

import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.games.provider.IGDBProvider;
import com.mayhew3.mediamogul.model.games.Game;
import com.mayhew3.mediamogul.model.games.PossibleGameMatch;
import com.mayhew3.mediamogul.xml.JSONReader;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class IGDBUpdater {
  private Game game;
  private String titleToSearch;

  private SQLConnection connection;
  private IGDBProvider igdbProvider;
  private JSONReader jsonReader;

  private Set<Integer> existingGameMatches = new HashSet<>();

  IGDBUpdater(@NotNull Game game, SQLConnection connection, IGDBProvider igdbProvider, JSONReader jsonReader) {
    this.game = game;
    this.connection = connection;
    this.igdbProvider = igdbProvider;
    this.jsonReader = jsonReader;

    String hint = game.igdb_hint.getValue();
    titleToSearch = hint == null ? game.title.getValue() : hint;
  }

  void updateGame() {
    updateExistingMatches();

    JSONArray gameMatches = igdbProvider.findGameMatches(getFormattedTitle());
    try {
      processPossibleMatches(gameMatches);
    } catch (UnsupportedOperationException | SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private String getFormattedTitle() {
    String formattedTitle = titleToSearch;
    formattedTitle = formattedTitle.replace("™", "");
    return formattedTitle;
  }

  private void processPossibleMatches(JSONArray results) throws SQLException {
    debug("Processing game: '" + game.title.getValue() + "', Formatted: '" + getFormattedTitle() + "'");

    Optional<JSONObject> exactMatch = findExactMatch(results);
    if (exactMatch.isPresent()) {
      debug(" - Exact match found!");
      saveExactMatch(exactMatch.get());
    } else {
      debug(" - No exact match.");
      savePossibleMatches(results);
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

    maybeUpdateGameWithBestMatch(results);
    game.igdb_failed.changeValue(new Date());
    game.commit(connection);
  }

  private void maybeUpdateGameWithBestMatch(JSONArray results) {
    if (results.length() > 0) {
      JSONObject firstMatch = results.getJSONObject(0);

      game.igdb_id.changeValue(jsonReader.getIntegerWithKey(firstMatch, "id"));
      game.igdb_title.changeValue(jsonReader.getStringWithKey(firstMatch, "name"));
    }
  }

  private void saveExactMatch(JSONObject exactMatch) throws SQLException {
    @NotNull Integer id = jsonReader.getIntegerWithKey(exactMatch, "id");
    String name = jsonReader.getStringWithKey(exactMatch, "name");

    game.igdb_id.changeValue(id);
    game.igdb_title.changeValue(name);
    game.igdb_success.changeValue(new Date());

    Optional<JSONObject> optionalCover = jsonReader.getOptionalObjectWithKey(exactMatch, "cover");
    if (optionalCover.isPresent()) {
      JSONObject cover = optionalCover.get();

      @NotNull String cloudinary_id = jsonReader.getStringWithKey(cover, "cloudinary_id");
      @NotNull Integer width = jsonReader.getIntegerWithKey(cover, "width");
      @NotNull Integer height = jsonReader.getIntegerWithKey(cover, "height");

      game.igdb_poster.changeValue(cloudinary_id);
      game.igdb_poster_w.changeValue(width);
      game.igdb_poster_h.changeValue(height);
    }

    game.commit(connection);
  }

  private void savePossibleMatch(JSONObject possibleMatch) throws SQLException {
    @NotNull Integer id = jsonReader.getIntegerWithKey(possibleMatch, "id");

    PossibleGameMatch possibleGameMatch = getOrCreateMatch(id);

    @NotNull String name = jsonReader.getStringWithKey(possibleMatch, "name");

    possibleGameMatch.gameId.changeValue(game.id.getValue());
    possibleGameMatch.igdbGameExtId.changeValue(id);
    possibleGameMatch.igdbGameTitle.changeValue(name);

    Optional<JSONObject> optionalCover = jsonReader.getOptionalObjectWithKey(possibleMatch, "cover");

    if (optionalCover.isPresent()) {
      JSONObject cover = optionalCover.get();
      @NotNull String cloudinary_id = jsonReader.getStringWithKey(cover, "cloudinary_id");
      @NotNull Integer width = jsonReader.getIntegerWithKey(cover, "width");
      @NotNull Integer height = jsonReader.getIntegerWithKey(cover, "height");

      possibleGameMatch.poster.changeValue(cloudinary_id);
      possibleGameMatch.poster_w.changeValue(width);
      possibleGameMatch.poster_h.changeValue(height);
    }

    possibleGameMatch.commit(connection);
  }

  private PossibleGameMatch getOrCreateMatch(Integer igdb_id) {
    try {
      PossibleGameMatch possibleGameMatch = new PossibleGameMatch();

      if (existingGameMatches.contains(igdb_id)) {
        ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
            "SELECT * " +
                "FROM possible_game_match " +
                "WHERE game_id = ? " +
                "AND igdb_game_ext_id = ? " +
                "AND retired = ? ",
            game.id.getValue(),
            igdb_id,
            0
        );

        if (resultSet.next()) {
          possibleGameMatch.initializeFromDBObject(resultSet);
        } else {
          throw new IllegalStateException("Found possible match on first pass with IGDB ID " + igdb_id + " and " +
              "Game ID " + game.id.getValue() + ", but no longer found.");
        }
      } else {
        possibleGameMatch.initializeForInsert();
      }
      return possibleGameMatch;
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void updateExistingMatches() {
    try {
      ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
          "SELECT igdb_game_ext_id " +
              "FROM possible_game_match " +
              "WHERE game_id = ? " +
              "AND retired = ? ", game.id.getValue(), 0);

      while (resultSet.next()) {
        Integer igdb_game_ext_id = resultSet.getInt("igdb_game_ext_id");
        existingGameMatches.add(igdb_game_ext_id);
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private Optional<JSONObject> findExactMatch(JSONArray possibleMatches) {
    String searchString = getFormattedTitle();

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
