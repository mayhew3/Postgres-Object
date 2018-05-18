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

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, gameID, "Played");
    while (resultSet.next()) {
      GameLog gameLog = new GameLog();
      gameLog.initializeFromDBObject(resultSet);

      if (previousGameLog != null && areDifferentSessions(previousGameLog, gameLog)) {
        createGameplaySession(connectedLogs);

        connectedLogs = new ArrayList<>();
      }

      connectedLogs.add(gameLog);
      previousGameLog = gameLog;
    }

    if (previousGameLog != null) {
      createGameplaySession(connectedLogs);
    }
  }

  private void createGameplaySession(@NotNull List<GameLog> connectedLogs) throws SQLException {
    GameplaySession gameplaySession = new GameplaySession();
    gameplaySession.initializeForInsert();

    gameplaySession.gameID.changeValue(getFirstLog(connectedLogs).gameID.getValue());
    gameplaySession.startTime.changeValue(getStartTime(connectedLogs));
    gameplaySession.minutes.changeValue(getTotalMinutes(connectedLogs));
    gameplaySession.currentlyPlaying.changeValue(false);

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
