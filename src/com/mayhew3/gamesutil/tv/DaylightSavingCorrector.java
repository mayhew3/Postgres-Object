package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.TiVoEpisode;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.TimeZone;

public class DaylightSavingCorrector {

  private SQLConnection connection;

  private DaylightSavingCorrector(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String... args) throws URISyntaxException, SQLException {
    String dbIdentifier = new ArgumentChecker(args).getDBIdentifier();
    SQLConnection connection = new PostgresConnectionFactory().createConnection(dbIdentifier);

    DaylightSavingCorrector upgrader = new DaylightSavingCorrector(connection);
    upgrader.correctDates();
  }

  private void correctDates() throws SQLException {
    LocalDate daylightDay = new LocalDate(2017, 3, 5);

    String sql =
        "select * " +
            "from tivo_episode " +
            "where date_added::date = ? " +
            "and capture_date < ? " +
            "and retired = ? " +
            "order by capture_date;";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql,
        toTimestamp(daylightDay),
        toTimestamp(daylightDay.minusDays(2)),
        0);
    while (resultSet.next()) {
      TiVoEpisode tiVoEpisode = new TiVoEpisode();
      tiVoEpisode.initializeFromDBObject(resultSet);

      debug("Processing episode: " + tiVoEpisode);

      DateTime correctCaptureDate = new DateTime(tiVoEpisode.captureDate.getValue().getTime());
      DateTime wrongCaptureDate = correctCaptureDate.plusHours(1);

      Optional<TiVoEpisode> duplicateEpisode = getDuplicateEpisode(
          tiVoEpisode,
          new Timestamp(wrongCaptureDate.getMillis()));

      if (duplicateEpisode.isPresent()) {
        debug("Found duplicate! Fixing dates...");
        tiVoEpisode.retire();
        tiVoEpisode.commit(connection);

        TiVoEpisode duplicate = duplicateEpisode.get();
        duplicate.captureDate.changeValue(tiVoEpisode.captureDate.getValue());
        duplicate.showingStartTime.changeValue(tiVoEpisode.showingStartTime.getValue());
        duplicate.commit(connection);

        unlinkFromEpisodes(tiVoEpisode);
      } else {
        debug("No duplicate found.");
      }

    }
  }

  private Timestamp toTimestamp(LocalDate localDate) {
    return new Timestamp(localDate.toDate().getTime());
  }

  private void unlinkFromEpisodes(TiVoEpisode tiVoEpisode) throws SQLException {
    String sql = "DELETE FROM edge_tivo_episode " +
        "WHERE tivo_episode_id = ? ";

    connection.prepareAndExecuteStatementUpdate(sql, tiVoEpisode.id.getValue());
  }

  private Optional<TiVoEpisode> getDuplicateEpisode(TiVoEpisode tiVoEpisode, Timestamp captureDate) throws SQLException {
    LocalDate safeZone = new LocalDate(2017, 3, 3);

    String sql =
        "select * " +
            "from tivo_episode " +
            "where program_v2_id = ? " +
            "and capture_date = ? " +
            "and (date_added is null or date_added < ?) " +
            "and retired = ? ";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        sql,
        tiVoEpisode.programV2Id.getValue(),
        captureDate,
        new Timestamp(safeZone.toDate().getTime()),
        0);

    if (resultSet.next()) {
      TiVoEpisode duplicate = new TiVoEpisode();
      duplicate.initializeFromDBObject(resultSet);
      if (resultSet.next()) {
        throw new RuntimeException("Found multiple duplicates for TiVoEpisode: " + tiVoEpisode);
      }
      return Optional.of(duplicate);
    } else {
      return Optional.empty();
    }
  }


  protected void debug(Object object) {
    System.out.println(object);
  }


}
