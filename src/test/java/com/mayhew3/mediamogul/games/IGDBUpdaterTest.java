package com.mayhew3.mediamogul.games;

import com.mayhew3.mediamogul.GamesDatabaseTest;
import com.mayhew3.mediamogul.games.provider.IGDBProvider;
import com.mayhew3.mediamogul.games.provider.IGDBTestProvider;
import com.mayhew3.mediamogul.model.games.Game;
import com.mayhew3.mediamogul.model.games.PossibleGameMatch;
import com.mayhew3.mediamogul.xml.JSONReaderImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IGDBUpdaterTest extends GamesDatabaseTest {

  private IGDBProvider igdbProvider;
  private JSONReaderImpl jsonReader;

  @Override
  public void setUp() throws URISyntaxException, SQLException {
    super.setUp();
    jsonReader = new JSONReaderImpl();
    igdbProvider = new IGDBTestProvider("src\\test\\resources\\IGDBTest\\", jsonReader);
  }

  @Test
  public void testManyResultsOneExactMatch() throws SQLException {
    String gameTitle = "Forza Horizon 4";

    Game game = createGame(gameTitle, "PC");

    IGDBUpdater igdbUpdater = new IGDBUpdater(game, connection, igdbProvider, jsonReader);
    igdbUpdater.updateGame();

    assertThat(game.igdb_id.getValue())
        .isEqualTo(82090);
    assertThat(game.igdb_title.getValue())
        .isEqualTo(gameTitle);
    assertThat(game.igdb_poster.getValue())
        .isEqualTo("ogznieioyzvsiok1sl2m");
    assertThat(game.igdb_poster_w.getValue())
        .isEqualTo(1440);
    assertThat(game.igdb_poster_h.getValue())
        .isEqualTo(2160);
    assertThat(game.igdb_failed.getValue())
        .isNull();
    assertThat(game.igdb_success.getValue())
        .isNotNull();

    List<PossibleGameMatch> possibleGameMatches = findPossibleGameMatches(game);
    assertThat(possibleGameMatches)
        .isEmpty();
  }

  @Test
  public void testTwoResultsTwoExactMatches() throws SQLException {
    String gameTitle = "Doom";

    Game game = createGame(gameTitle, "PC");

    IGDBUpdater igdbUpdater = new IGDBUpdater(game, connection, igdbProvider, jsonReader);
    igdbUpdater.updateGame();

    assertThat(game.igdb_id.getValue())
        .isEqualTo(82090);
    assertThat(game.igdb_title.getValue())
        .isEqualTo(gameTitle);
    assertThat(game.igdb_poster.getValue())
        .isNull();
    assertThat(game.igdb_poster_w.getValue())
        .isNull();
    assertThat(game.igdb_poster_h.getValue())
        .isNull();
    assertThat(game.igdb_failed.getValue())
        .isNotNull();
    assertThat(game.igdb_success.getValue())
        .isNull();

    List<PossibleGameMatch> possibleGameMatches = findPossibleGameMatches(game);
    assertThat(possibleGameMatches)
        .hasSize(2);

  }

  @Test
  public void testNoResults() throws SQLException {
    String gameTitle = "Gorfond";

    Game game = createGame(gameTitle, "Xbox One");

    IGDBUpdater igdbUpdater = new IGDBUpdater(game, connection, igdbProvider, jsonReader);
    igdbUpdater.updateGame();

    assertThat(game.igdb_id.getValue())
        .isNull();
    assertThat(game.igdb_title.getValue())
        .isNull();
    assertThat(game.igdb_poster.getValue())
        .isNull();
    assertThat(game.igdb_poster_w.getValue())
        .isNull();
    assertThat(game.igdb_poster_h.getValue())
        .isNull();
    assertThat(game.igdb_failed.getValue())
        .isNotNull();
    assertThat(game.igdb_success.getValue())
        .isNull();

    List<PossibleGameMatch> possibleGameMatches = findPossibleGameMatches(game);
    assertThat(possibleGameMatches)
        .isEmpty();
  }


  @Test
  public void testManyResultsNoExactMatch() throws SQLException {
    String gameTitle = "Forza Horizon 4 Awesome";

    Game game = createGame(gameTitle, "Xbox One");

    IGDBUpdater igdbUpdater = new IGDBUpdater(game, connection, igdbProvider, jsonReader);
    igdbUpdater.updateGame();

    assertThat(game.igdb_id.getValue())
        .isEqualTo(82090);
    assertThat(game.igdb_title.getValue())
        .isEqualTo("Forza Horizon 4");
    assertThat(game.igdb_poster.getValue())
        .isNull();
    assertThat(game.igdb_poster_w.getValue())
        .isNull();
    assertThat(game.igdb_poster_h.getValue())
        .isNull();
    assertThat(game.igdb_failed.getValue())
        .isNotNull();
    assertThat(game.igdb_success.getValue())
        .isNull();

    List<PossibleGameMatch> possibleGameMatches = findPossibleGameMatches(game);
    assertThat(possibleGameMatches)
        .hasSize(5);

    PossibleGameMatch firstMatch = possibleGameMatches.get(0);
    assertThat(firstMatch.igdbGameTitle.getValue())
        .isEqualTo("Forza Horizon 4");
    assertThat(firstMatch.igdbGameExtId.getValue())
        .isEqualTo(82090);
    assertThat(firstMatch.poster.getValue())
        .isEqualTo("ogznieioyzvsiok1sl2m");
    assertThat(firstMatch.poster_w.getValue())
        .isEqualTo(1440);
    assertThat(firstMatch.poster_h.getValue())
        .isEqualTo(2160);
  }



  // utility methods
  private Game createGame(String gameName, @NotNull String platform) throws SQLException {
    Game game = new Game();
    game.initializeForInsert();
    game.title.changeValue(gameName);
    game.platform.changeValue(platform);

    game.commit(connection);

    return game;
  }


  @NotNull
  private List<PossibleGameMatch> findPossibleGameMatches(Game game) throws SQLException {
    List<PossibleGameMatch> results = new ArrayList<>();
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM possible_game_match " +
            "WHERE game_id = ? " +
            "ORDER BY id ", game.id.getValue()
    );
    while (resultSet.next()) {
      PossibleGameMatch possibleGameMatch = new PossibleGameMatch();
      possibleGameMatch.initializeFromDBObject(resultSet);
      results.add(possibleGameMatch);
    }
    return results;
  }
}
