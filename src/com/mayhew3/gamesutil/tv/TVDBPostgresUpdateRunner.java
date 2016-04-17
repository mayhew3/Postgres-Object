package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.dataobject.SeriesPostgres;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.NodeReaderImpl;
import com.mongodb.MongoException;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TVDBPostgresUpdateRunner {

  private Integer seriesUpdates = 0;
  private Integer episodesAdded = 0;
  private Integer episodesUpdated = 0;

  private SQLConnection connection;

  public TVDBPostgresUpdateRunner(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String[] args) throws URISyntaxException, SQLException {
    SQLConnection connection = new PostgresConnectionFactory().createConnection();
    TVDBPostgresUpdateRunner tvdbUpdateRunner = new TVDBPostgresUpdateRunner(connection);
    tvdbUpdateRunner.runUpdate();
  }

  public void runUpdate() throws SQLException {

    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ?\n" +
        "and suggestion = ?";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, false);

    int totalRows = resultSet.getFetchSize();
    debug(totalRows + " series found for update. Starting.");

    int i = 0;

    try {
      while (resultSet.next()) {
        i++;
        SeriesPostgres series = new SeriesPostgres();
        series.initializeFromDBObject(resultSet);

        try {
          updateMetacritic(series);
        } catch (RuntimeException | ShowFailedException e) {
          e.printStackTrace();
          debug("Show failed metacritic: " + series.seriesTitle.getValue());
        }

        try {
          updateTVDB(series);
        } catch (ShowFailedException | BadlyFormattedXMLException e) {
          e.printStackTrace();
          debug("Show failed TVDB: " + series.seriesTitle.getValue());
        }

        seriesUpdates++;
        debug(i + " processed.");
      }
    } catch (MongoException e) {
      debug("Threw weird exception!");
    }
  }

  private void updateTVDB(SeriesPostgres series) throws SQLException, BadlyFormattedXMLException, ShowFailedException {
    TVDBSeriesPostgresUpdater updater = new TVDBSeriesPostgresUpdater(connection, series, new NodeReaderImpl());
    updater.updateSeries();

    episodesAdded += updater.getEpisodesAdded();
    episodesUpdated += updater.getEpisodesUpdated();
  }

  private void updateMetacritic(SeriesPostgres series) throws ShowFailedException {
    MetacriticTVPostgresUpdater metacriticUpdater = new MetacriticTVPostgresUpdater(series, connection);
    metacriticUpdater.runUpdater();
  }


  public Integer getSeriesUpdates() {
    return seriesUpdates;
  }

  public Integer getEpisodesAdded() {
    return episodesAdded;
  }

  public Integer getEpisodesUpdated() {
    return episodesUpdated;
  }



  protected void debug(Object object) {
    System.out.println(object);
  }

}

