package com.mayhew3.mediamogul.games;

import com.mayhew3.mediamogul.ArgumentChecker;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.model.games.GameLog;
import com.mayhew3.mediamogul.model.games.GameplaySession;
import com.mayhew3.mediamogul.scheduler.UpdateRunner;
import com.mayhew3.mediamogul.tv.helper.UpdateMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SteamPlaySessionGenerator implements UpdateRunner {

  private SQLConnection connection;

  private SteamPlaySessionGenerator(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String[] args) throws SQLException, URISyntaxException {
    ArgumentChecker argumentChecker = new ArgumentChecker(args);

    SQLConnection connection = PostgresConnectionFactory.createConnection(argumentChecker);

    SteamPlaySessionGenerator updateRunner = new SteamPlaySessionGenerator(connection);
    updateRunner.runUpdate();
  }


  @Override
  public void runUpdate() throws SQLException {
    String sql = "SELECT DISTINCT game_id " +
        "FROM game_log " +
        "WHERE eventtype = ? " +
        "AND gameplay_session_id IS NULL " +
        "ORDER BY game_id ";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, "Played");

    while (resultSet.next()) {
      int gameID = resultSet.getInt("game_id");
      updateSessions(gameID);
    }
  }

  private void updateSessions(Integer gameID) throws SQLException {
    String sql = "SELECT * " +
        "FROM game_log " +
        "WHERE game_id = ? " +
        "AND eventtype = ? " +
        "AND gameplay_session_id IS NULL " +
        "ORDER BY eventdate ";

    GameLog previousGameLog = null;
    List<GameLog> connectedLogs = new ArrayList<>();
    GameplaySession sessionToContinue = null;

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, gameID, "Played");
    while (resultSet.next()) {
      GameLog gameLog = new GameLog();
      gameLog.initializeFromDBObject(resultSet);

      if (previousGameLog == null) {
        // check if we can append this to an existing session.
        sessionToContinue = findSessionToContinue(gameLog);
      } else {
        if (areDifferentSessions(previousGameLog, gameLog)) {
          if (sessionToContinue == null) {
            createGameplaySession(connectedLogs);
          } else {
            appendToGameplaySession(sessionToContinue, connectedLogs);
            sessionToContinue = null;
          }
          connectedLogs = new ArrayList<>();
        }
      }

      connectedLogs.add(gameLog);
      previousGameLog = gameLog;
    }

    if (previousGameLog != null) {
      if (sessionToContinue == null) {
        createGameplaySession(connectedLogs);
      } else {
        appendToGameplaySession(sessionToContinue, connectedLogs);
      }
    }
  }

  private void appendToGameplaySession(@NotNull GameplaySession gameplaySession, @NotNull List<GameLog> connectedLogs) throws SQLException {
    Integer initialMinutes = getInitialMinutes(gameplaySession);
    if (initialMinutes == null) {
      throw new IllegalStateException("No previousplaytime found with gameplay_session_id " + gameplaySession.id.getValue());
    }

    GameLog lastLog = getLastLog(connectedLogs);

    Integer totalMinutes = lastLog.updatedplaytime.getValue().intValue() - initialMinutes;

    gameplaySession.minutes.changeValue(totalMinutes);
    gameplaySession.commit(connection);

    markGameLogsUsed(connectedLogs, gameplaySession);
  }

  private @Nullable Integer getInitialMinutes(@NotNull GameplaySession gameplaySession) throws SQLException {
    String sql = "SELECT MIN(previousplaytime) AS initial_playtime " +
        "FROM game_log " +
        "WHERE gameplay_session_id = ? ";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, gameplaySession.id.getValue());
    if (resultSet.next()) {
      return resultSet.getBigDecimal("initial_playtime").intValue();
    } else {
      return null;
    }
  }

  private @Nullable GameplaySession findSessionToContinue(@NotNull GameLog gameLog) throws SQLException {
    DateTime finishTime = new DateTime(gameLog.eventdate.getValue());
    Integer diff = gameLog.diff.getValue().intValue();

    DateTime threshold = finishTime.minusMinutes(diff + 30);

    String sql = "SELECT gameplay_session_id, MAX(eventdate) " +
        "FROM game_log " +
        "WHERE game_id = ? " +
        "AND eventdate > ? " +
        "AND gameplay_session_id IS NOT NULL " +
        "GROUP BY gameplay_session_id " +
        "ORDER BY MAX(eventdate) DESC ";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, gameLog.gameID.getValue(),
        new Timestamp(threshold.toDate().getTime()));

    if (resultSet.next()) {
      Integer gameplaySessionId = resultSet.getInt("gameplay_session_id");
      GameplaySession foundSession = getGameplaySessionWithID(gameplaySessionId);

      if (foundSession == null) {
        throw new IllegalStateException("game_log found with gameplay_session_id " + gameplaySessionId + " that doesn't correspond to a gameplay_session.id.");
      }

      return foundSession;
    }

    return null;
  }

  private @Nullable GameplaySession getGameplaySessionWithID(@NotNull Integer id) throws SQLException {
    String sql = "SELECT * " +
        "FROM gameplay_session " +
        "WHERE id = ? ";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, id);
    if (resultSet.next()) {
      GameplaySession gameplaySession = new GameplaySession();
      gameplaySession.initializeFromDBObject(resultSet);
      return gameplaySession;
    } else {
      return null;
    }
  }

  private void createGameplaySession(@NotNull List<GameLog> connectedLogs) throws SQLException {
    GameplaySession gameplaySession = new GameplaySession();
    gameplaySession.initializeForInsert();

    gameplaySession.gameID.changeValue(getFirstLog(connectedLogs).gameID.getValue());
    gameplaySession.startTime.changeValue(getStartTime(connectedLogs));
    gameplaySession.minutes.changeValue(getTotalMinutes(connectedLogs));
    gameplaySession.manualAdjustment.changeValue(0);

    gameplaySession.commit(connection);

    markGameLogsUsed(connectedLogs, gameplaySession);
  }

  private Timestamp getStartTime(@NotNull List<GameLog> connectedLogs) {
    GameLog firstLog = connectedLogs.get(0);

    DateTime eventDate = new DateTime(firstLog.eventdate.getValue());
    Integer diff = firstLog.diff.getValue().intValue();

    return new Timestamp(eventDate.minusMinutes(diff).toDate().getTime());
  }

  private Integer getTotalMinutes(List<GameLog> connectedLogs) {
    GameLog firstLog = getFirstLog(connectedLogs);
    GameLog lastLog = getLastLog(connectedLogs);

    Integer initialMinutes = firstLog.previousPlaytime.getValue().intValue();
    Integer finalMinutes = lastLog.updatedplaytime.getValue().intValue();

    return finalMinutes - initialMinutes;
  }

  private GameLog getFirstLog(@NotNull List<GameLog> connectedLogs) {
    return connectedLogs.get(0);
  }

  private GameLog getLastLog(@NotNull List<GameLog> connectedLogs) {
    return connectedLogs.get(connectedLogs.size() - 1);
  }


  private void markGameLogsUsed(@NotNull List<GameLog> connectedLogs, @NotNull GameplaySession gameplaySession) throws SQLException {
    for (GameLog gameLog : connectedLogs) {
      gameLog.gameplaySessionID.changeValue(gameplaySession.id.getValue());
      gameLog.commit(connection);
    }
  }

  private Boolean areDifferentSessions(@NotNull GameLog olderGameLog, @NotNull GameLog newerGameLog) {
    Integer diff = newerGameLog.diff.getValue().intValue();
    Integer expandedDiff = diff + 30;

    DateTime newerGameTime = new DateTime(newerGameLog.eventdate.getValue().getTime());
    DateTime olderGameTime = new DateTime(olderGameLog.eventdate.getValue().getTime());

    return newerGameTime.minusMinutes(expandedDiff).isAfter(olderGameTime);
  }

  @Override
  public String getRunnerName() {
    return "Steam Play Session Generator";
  }

  @Override
  public @Nullable UpdateMode getUpdateMode() {
    return null;
  }

}
