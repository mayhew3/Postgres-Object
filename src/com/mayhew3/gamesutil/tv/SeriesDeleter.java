package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.Series;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Optional;

public class SeriesDeleter {

  private Series series;
  private SQLConnection connection;

  public SeriesDeleter(Series series, SQLConnection connection) {
    this.series = series;
    this.connection = connection;
  }

  public static void main(String... args) throws URISyntaxException, SQLException {
    String seriesTitle = "HUMANS";

    String dbIdentifier = new ArgumentChecker(args).getDBIdentifier();

    SQLConnection connection = new PostgresConnectionFactory().createConnection(dbIdentifier);

    Optional<Series> series = Series.findSeries(seriesTitle, connection);

    if (series.isPresent()) {
      SeriesDeleter seriesDeleter = new SeriesDeleter(series.get(), connection);
      seriesDeleter.executeDelete();
    } else {
      throw new RuntimeException("Unable to find series with title: " + seriesTitle);
    }
  }

  public void executeDelete() throws SQLException {
    /*
      - series
      |-- episode_group_rating
      |-- tvdb_series
        |-- tvdb_migration_log
        |-- tvdb_poster
      |-- season
        |-- season_viewing_location
      |-- possible_series_match
      |-- episode
        |-- tvdb_episode
        |-- edge_tivo_episode
          |-- tivo_episode
        |-- episode_rating
      |-- series_genre
      |-- series_viewing_location
      |-- tvdb_migration_error
      |-- tvdb_work_item
     */
/*

    deleteRowsWithFKToSeries("episode_group_rating");
    deleteRowsWithFKToSeries("error_log");
    deleteRowsWithFKToSeries("metacritic_season");
    deleteRowsWithFKToSeries("possible_series_match");
    deleteRowsWithFKToSeries("series_genre");
    deleteRowsWithFKToSeries("series_viewing_location");
    deleteRowsWithFKToSeries("tvdb_migration_error");
    deleteRowsWithFKToSeries("tvdb_work_item");


    // NOTE: has another dependency on episode. Must do after episodes are deleted.
    deleteSeasons();
*/

    Integer tvdbSeriesId = series.tvdbSeriesId.getValue();
    if (tvdbSeriesId == null) {
      Integer rowsDeleted = connection.prepareAndExecuteStatementUpdate("DELETE FROM series WHERE id = ?", series.id.getValue());
      debug(rowsDeleted + " rows deleted from series");
    } else {
      Integer rowsDeleted = connection.prepareAndExecuteStatementUpdate("DELETE FROM tvdb_series WHERE id = ?", tvdbSeriesId);
      debug(rowsDeleted + " rows deleted from tvdb_series");
    }
//    deleteTVDBSeries();
  }

  private void deleteEpisodes() throws SQLException {

    deleteRowsWithFKToSeries("episode");
  }

  // todo: delete if we end up not needing it.
  private void deleteTVDBEpisodes() throws SQLException {
    String sql = "delete from tvdb_episode te " +
        "using tvdb_series ts " +
        "inner join series s " +
        " on s.tvdb_series_id = ts.id " +
        "where te.tvdb_series_id = ts.id " +
        "and s.id = ?";
    Integer rowsDeleted = connection.prepareAndExecuteStatementUpdate(sql, series.id.getValue());

    debug("Deleted " + rowsDeleted + " rows from table tvdb_episode connected through tvdb_series.");
  }

  private void deleteSeasons() throws SQLException {
    deleteSeasonViewingLocations();
    deleteRowsWithFKToSeries("season");
  }

  private void deleteTVDBSeries() throws SQLException {
//    deleteTVDBSeriesChildRows("tvdb_migration_log");
//    deleteTVDBSeriesChildRows("tvdb_poster");
//    deleteTVDBSeriesChildRows("tvdb_episode");
    deleteRowsWithFKToSeries("tvdb_series");
  }

  private void deleteTVDBSeriesChildRows(String tableName) throws SQLException {
    String sql = "DELETE FROM " + tableName + " tml " +
        "USING series s " +
        "WHERE tml.tvdb_series_id = s.tvdb_series_id " +
        "AND s.id = ?";
    Integer rowsDeleted = connection.prepareAndExecuteStatementUpdate(sql, series.id.getValue());

    debug("Deleted " + rowsDeleted + " rows from table " + tableName + " connected through tvdb_series.");
  }

  private void deleteSeasonViewingLocations() throws SQLException {
    String sql = "DELETE FROM season_viewing_location svl " +
        "USING season s " +
        "WHERE svl.season_id = s.id " +
        "AND s.series_id = ?";
    Integer rowsDeleted = connection.prepareAndExecuteStatementUpdate(sql, series.id.getValue());

    debug("Deleted " + rowsDeleted + " rows from table season_viewing_location connected through season.");
  }

  private void deleteRowsWithFKToSeries(String tableName) throws SQLException {
    Integer series_id = series.id.getValue();
    Integer rowsDeleted = deleteRowsFromTableMatchingColumn(tableName, "series_id", series_id);

    debug("Deleted " + rowsDeleted + " rows from table " + tableName + " with series_id " + series_id);
  }

  private Integer deleteRowsFromTableMatchingColumn(String tableName, String columnName, Integer id) throws SQLException {
    String sql =
        "DELETE FROM " + tableName + " " +
        "WHERE " + columnName + " = ? " +
        "RETURNING * ";
    return connection.prepareAndExecuteStatementUpdate(sql, id);
  }


  protected void debug(Object object) {
    System.out.println(object);
  }

}
