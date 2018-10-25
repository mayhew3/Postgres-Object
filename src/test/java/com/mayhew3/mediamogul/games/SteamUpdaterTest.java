package com.mayhew3.mediamogul.games;

import com.mayhew3.mediamogul.DatabaseTest;
import com.mayhew3.mediamogul.games.provider.SteamTestProviderImpl;
import com.mayhew3.mediamogul.model.Person;
import com.mayhew3.mediamogul.model.games.Game;
import com.mayhew3.mediamogul.model.games.GameLog;
import com.mayhew3.mediamogul.model.games.PersonGame;
import com.mayhew3.mediamogul.xml.JSONReader;
import com.mayhew3.mediamogul.xml.JSONReaderImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.fest.assertions.api.Assertions.assertThat;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class SteamUpdaterTest extends DatabaseTest {
  private JSONReader jsonReader;
  private SteamTestProviderImpl steamProvider;
  private int person_id;

  @Override
  public void setUp() throws URISyntaxException, SQLException {
    super.setUp();
    jsonReader = new JSONReaderImpl();
    steamProvider = new SteamTestProviderImpl("src\\test\\resources\\Steam\\steam_", jsonReader);
    person_id = 1;
    createPerson();
  }

  @Test
  public void testNewSteamGame() throws SQLException {
    steamProvider.setFileSuffix("xcom2");
    String gameName = "XCOM 2";
    int playtime = 11558;
    int steamID = 268500;

    createOwnedGame("Clunkers", "Steam", 48762);

    SteamGameUpdater steamGameUpdater = new SteamGameUpdater(connection, person_id, steamProvider);
    steamGameUpdater.runUpdate();


    Optional<Game> optionalGame = findGameFromDB(gameName);

    assertThat(optionalGame.isPresent())
        .as("Expected game XCOM 2 to exist in database.")
        .isTrue();

    Game game = optionalGame.get();

    Optional<PersonGame> optionalPersonGame = findPersonGame(game);

    assertThat(optionalPersonGame.isPresent())
        .isTrue();

    PersonGame personGame = optionalPersonGame.get();

    assertThat(game.steam_title.getValue())
        .isEqualTo(gameName);
    assertThat(game.steamID.getValue())
        .isEqualTo(268500);
    assertThat(game.icon.getValue())
        .isEqualTo("f275aeb0b1b947262810569356a199848c643754");
    assertThat(game.logo.getValue())
        .isEqualTo("10a6157d6614f63cd8a95d002d022778c207c218");
    assertThat(game.playtime.getValue())
        .isEqualByComparingTo(new BigDecimal(playtime));
    assertThat(game.owned.getValue())
        .isEqualTo("owned");
    assertThat(game.metacriticPage.getValue())
        .isFalse();
  /*  assertThat(game.started.getValue())
        .isTrue();*/

    assertThat(personGame.minutes_played.getValue())
        .isEqualTo(playtime);
    assertThat(personGame.tier.getValue())
        .isEqualTo(2);

    List<GameLog> gameLogs = findGameLogs(game);
    assertThat(gameLogs)
        .hasSize(1);

    GameLog gameLog = gameLogs.get(0);
    assertThat(gameLog.game.getValue())
        .isEqualTo(gameName);
    assertThat(gameLog.steamID.getValue())
        .isEqualTo(steamID);
    assertThat(gameLog.platform.getValue())
        .isEqualTo("Steam");
    assertThat(gameLog.previousPlaytime.getValue())
        .isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(gameLog.updatedplaytime.getValue())
        .isEqualByComparingTo(new BigDecimal(playtime));
    assertThat(gameLog.diff.getValue())
        .isEqualByComparingTo(new BigDecimal(playtime));
    assertThat(gameLog.eventdate.getValue())
        .isNotNull();
    assertThat(gameLog.eventtype.getValue())
        .isEqualTo("Played");
  }

  @Test
  public void testModifySteamGame() throws SQLException {
    steamProvider.setFileSuffix("xcom2");
    String gameName = "XCOM 2";
    int playtime = 11558;
    int steamID = 268500;

    createOwnedGame(gameName, "Steam", steamID);

    SteamGameUpdater steamGameUpdater = new SteamGameUpdater(connection, person_id, steamProvider);
    steamGameUpdater.runUpdate();


    Optional<Game> optionalGame = findGameFromDB(gameName);

    assertThat(optionalGame.isPresent())
        .as("Expected game XCOM 2 to exist in database.")
        .isTrue();

    Game game = optionalGame.get();

    Optional<PersonGame> optionalPersonGame = findPersonGame(game);

    assertThat(optionalPersonGame.isPresent())
        .isTrue();

    PersonGame personGame = optionalPersonGame.get();

    assertThat(game.steam_title.getValue())
        .isEqualTo(gameName);

    assertThat(game.steamID.getValue())
        .isEqualTo(steamID);
    assertThat(game.icon.getValue())
        .isEqualTo("f275aeb0b1b947262810569356a199848c643754");
    assertThat(game.logo.getValue())
        .isEqualTo("10a6157d6614f63cd8a95d002d022778c207c218");
    assertThat(game.playtime.getValue())
        .isEqualByComparingTo(new BigDecimal(playtime));
    assertThat(game.owned.getValue())
        .isEqualTo("owned");
    assertThat(game.metacriticPage.getValue())
        .isFalse();

    assertThat(personGame.minutes_played.getValue())
        .isEqualTo(playtime);
    assertThat(personGame.tier.getValue())
        .isEqualTo(2);

    List<GameLog> gameLogs = findGameLogs(game);
    assertThat(gameLogs)
        .hasSize(1);

    GameLog gameLog = gameLogs.get(0);
    assertThat(gameLog.game.getValue())
        .isEqualTo(gameName);
    assertThat(gameLog.steamID.getValue())
        .isEqualTo(steamID);
    assertThat(gameLog.platform.getValue())
        .isEqualTo("Steam");
    assertThat(gameLog.previousPlaytime.getValue())
        .isEqualByComparingTo(new BigDecimal(10234));
    assertThat(gameLog.updatedplaytime.getValue())
        .isEqualByComparingTo(new BigDecimal(playtime));
    assertThat(gameLog.diff.getValue())
        .isEqualByComparingTo(new BigDecimal(playtime - 10234));
    assertThat(gameLog.eventdate.getValue())
        .isNotNull();
    assertThat(gameLog.eventtype.getValue())
        .isEqualTo("Played");

  }

  @Test
  public void testModifyDoesntChangeName() throws SQLException {
    steamProvider.setFileSuffix("xcom2");
    String steamName = "XCOM 2";
    String myName = "X-Com 2";

    int steamID = 268500;

    createOwnedGame(myName, "Steam", steamID);

    SteamGameUpdater steamGameUpdater = new SteamGameUpdater(connection, person_id, steamProvider);
    steamGameUpdater.runUpdate();

    Optional<Game> optionalGame = findGameFromDB(myName);

    assertThat(optionalGame.isPresent())
        .as("Expected game XCOM 2 to exist in database.")
        .isTrue();

    Game game = optionalGame.get();

    assertThat(game.steam_title.getValue())
        .isEqualTo(steamName);
    assertThat(game.title.getValue())
        .isEqualTo(myName);

    List<GameLog> gameLogs = findGameLogs(game);
    assertThat(gameLogs)
        .hasSize(1);

    GameLog gameLog = gameLogs.get(0);
    assertThat(gameLog.game.getValue())
        .isEqualTo(steamName);
  }

  @Test
  public void testSteamGameChangedToNotOwned() throws SQLException {
    steamProvider.setFileSuffix("xcom2");

    createOwnedGame("Clunkers", "Steam", 48762);

    SteamGameUpdater steamGameUpdater = new SteamGameUpdater(connection, person_id, steamProvider);
    steamGameUpdater.runUpdate();


    Optional<Game> optionalGame = findGameFromDB("Clunkers");

    assertThat(optionalGame.isPresent())
        .as("Expected game Clunkers to exist in database.")
        .isTrue();

    Game game = optionalGame.get();

    assertThat(game.owned.getValue())
        .isEqualTo("not owned");

    Optional<PersonGame> optionalPersonGame = game.getPersonGame(person_id, connection);

    assertThat(optionalPersonGame.isPresent())
        .isFalse();

  }

  @Test
  public void testUnlinkThenLinkSteamGame() throws SQLException {
    steamProvider.setFileSuffix("xcom2");

    createOwnedGame("Clunkers", "Steam", 48762);

    SteamGameUpdater steamGameUpdater = new SteamGameUpdater(connection, person_id, steamProvider);
    steamGameUpdater.runUpdate();


    Optional<Game> optionalGame = findGameFromDB("Clunkers");

    assertThat(optionalGame.isPresent())
        .as("Expected game Clunkers to exist in database.")
        .isTrue();

    Game game = optionalGame.get();

    assertThat(game.owned.getValue())
        .isEqualTo("not owned");

    Optional<PersonGame> optionalPersonGame = game.getPersonGame(person_id, connection);

    assertThat(optionalPersonGame.isPresent())
        .isFalse();

    steamProvider.setFileSuffix("clunkers");

    steamGameUpdater.runUpdate();

    optionalGame = findGameFromDB("Clunkers");

    assertThat(optionalGame.isPresent())
        .as("Expected game Clunkers to exist in database.")
        .isTrue();

    game = optionalGame.get();

    assertThat(game.owned.getValue())
        .isEqualTo("owned");

    optionalPersonGame = game.getPersonGame(person_id, connection);

    assertThat(optionalPersonGame.isPresent())
        .isTrue();

    PersonGame personGame = optionalPersonGame.get();

    assertThat(personGame.minutes_played.getValue())
        .isEqualTo(10234);
    assertThat(personGame.tier.getValue())
        .isEqualTo(2);
  }

  // utility methods

  private void createPerson() throws SQLException {
    Person person = new Person();
    person.initializeForInsert();
    person.id.changeValue(person_id);
    person.email.changeValue("fake@notreal.com");
    person.firstName.changeValue("Mayhew");
    person.lastName.changeValue("Fakename");

    person.commit(connection);
  }

  private Game createOwnedGame(String gameName, @NotNull String platform, Integer steamID) throws SQLException {
    Game game = new Game();
    game.initializeForInsert();
    game.title.changeValue(gameName);
    game.platform.changeValue(platform);
    game.steamID.changeValue(steamID);
    game.steam_title.changeValue(gameName);
    game.owned.changeValue("owned");

    game.commit(connection);

    PersonGame personGame = new PersonGame();
    personGame.initializeForInsert();
    personGame.game_id.changeValue(game.id.getValue());
    personGame.person_id.changeValue(person_id);
    personGame.tier.changeValue(2);
    personGame.minutes_played.changeValue(10234);

    personGame.commit(connection);

    return game;
  }



  private Optional<Game> findGameFromDB(String gameName) throws SQLException {
    String sql = "SELECT * " +
        "FROM game " +
        "WHERE title = ? ";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, gameName);

    if (resultSet.next()) {
      Game game = new Game();
      game.initializeFromDBObject(resultSet);
      return Optional.of(game);
    } else {
      return Optional.empty();
    }
  }

  private Optional<PersonGame> findPersonGame(Game game) throws SQLException {
    String sql = "SELECT * " +
        "FROM person_game " +
        "WHERE game_id = ? " +
        "AND person_id = ? " +
        "AND retired = ? ";


    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, game.id.getValue(), person_id, 0);

    if (resultSet.next()) {
      PersonGame personGame = new PersonGame();
      personGame.initializeFromDBObject(resultSet);
      return Optional.of(personGame);
    } else {
      return Optional.empty();
    }
  }

  private List<GameLog> findGameLogs(Game game) throws SQLException {
    List<GameLog> gameLogs = new ArrayList<>();

    String sql = "SELECT * " +
        "FROM game_log " +
        "WHERE game_id = ? " +
        "AND person_id = ? ";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, game.id.getValue(), person_id);

    while (resultSet.next()) {
      GameLog gameLog = new GameLog();
      gameLog.initializeFromDBObject(resultSet);
      gameLogs.add(gameLog);
    }

    return gameLogs;
  }

}
