package com.mayhew3.mediamogul.games;

import com.mayhew3.mediamogul.DatabaseTest;
import com.mayhew3.mediamogul.games.provider.SteamTestProviderImpl;
import com.mayhew3.mediamogul.model.Person;
import com.mayhew3.mediamogul.model.games.Game;
import com.mayhew3.mediamogul.model.games.PersonGame;
import com.mayhew3.mediamogul.xml.JSONReader;
import com.mayhew3.mediamogul.xml.JSONReaderImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    createGame("Clunkers", "Steam");

    SteamGameUpdater steamGameUpdater = new SteamGameUpdater(connection, person_id, steamProvider);
    steamGameUpdater.runUpdate();


    Optional<Game> optionalGame = findGameFromDB(gameName);

    assertThat(optionalGame.isPresent())
        .as("Expected game XCOM 2 to exist in database.")
        .isTrue();

    Game createdGame = optionalGame.get();

    Optional<PersonGame> optionalPersonGame = findPersonGame(createdGame);

    assertThat(optionalPersonGame.isPresent())
        .isTrue();

    PersonGame personGame = optionalPersonGame.get();

    assertThat(createdGame.steam_title.getValue())
        .isEqualTo(gameName);
    assertThat(createdGame.steamID.getValue())
        .isEqualTo(268500);
    assertThat(createdGame.icon.getValue())
        .isEqualTo("f275aeb0b1b947262810569356a199848c643754");
    assertThat(createdGame.logo.getValue())
        .isEqualTo("10a6157d6614f63cd8a95d002d022778c207c218");

    assertThat(personGame.minutes_played.getValue())
        .isEqualTo(11558);

  }

  @Test
  public void testModifySteamGame() {

  }

  // utility methods

  private Person createPerson() throws SQLException {
    Person person = new Person();
    person.initializeForInsert();
    person.id.changeValue(person_id);
    person.email.changeValue("fake@notreal.com");
    person.firstName.changeValue("Mayhew");
    person.lastName.changeValue("Fakename");

    person.commit(connection);

    return person;
  }

  private Game createGame(String gameName, @NotNull String platform) throws SQLException {
    Game game = new Game();
    game.initializeForInsert();
    game.title.changeValue(gameName);
    game.platform.changeValue(platform);

    game.commit(connection);

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

}
