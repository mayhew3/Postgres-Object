package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.dataobject.Series;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.NodeReaderImpl;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TVDBUpdateRunner {

  private Integer seriesUpdates = 0;
  private Integer episodesAdded = 0;
  private Integer episodesUpdated = 0;

  private SQLConnection connection;

  public TVDBUpdateRunner(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String[] args) throws URISyntaxException, SQLException {
    SQLConnection connection = new PostgresConnectionFactory().createConnection();
    TVDBUpdateRunner tvdbUpdateRunner = new TVDBUpdateRunner(connection);
    tvdbUpdateRunner.runUpdate();

    // update denorms after changes.
    new SeriesDenormUpdater(connection).updateFields();
  }

  /**
   * Go to theTVDB and update all series in my DB with the ones from theirs.
   * 
   * @throws SQLException if query to get series to update fails. Any one series update will not halt operation of the
   *                    script, but if the query to find all the serieses fails, the operation can't continue.
   */
  public void runUpdate() throws SQLException {

    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ?";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false);

    int totalRows = resultSet.getFetchSize();
    debug(totalRows + " series found for update. Starting.");

    int i = 0;

    while (resultSet.next()) {
      i++;
      Series series = new Series();
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
  }

  private void updateTVDB(Series series) throws SQLException, BadlyFormattedXMLException, ShowFailedException {
    TVDBSeriesUpdater updater = new TVDBSeriesUpdater(connection, series, new NodeReaderImpl());
    updater.updateSeries();

    episodesAdded += updater.getEpisodesAdded();
    episodesUpdated += updater.getEpisodesUpdated();
  }

  private void updateMetacritic(Series series) throws ShowFailedException {
    MetacriticTVUpdater metacriticUpdater = new MetacriticTVUpdater(series, connection);
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

