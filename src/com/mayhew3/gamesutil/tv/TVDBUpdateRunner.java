package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.dataobject.Series;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.NodeReaderImpl;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TVDBUpdateRunner {

  private Integer seriesUpdates = 0;
  private Integer episodesAdded = 0;
  private Integer episodesUpdated = 0;

  private SQLConnection connection;

  public TVDBUpdateRunner(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String... args) throws URISyntaxException, SQLException {
    List<String> argList = Lists.newArrayList(args);
    Boolean singleSeries = argList.contains("SingleSeries");
    String identifier = new ArgumentChecker(args).getDBIdentifier();

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);
    TVDBUpdateRunner tvdbUpdateRunner = new TVDBUpdateRunner(connection);

    if (singleSeries) {
      tvdbUpdateRunner.runUpdateSingle();
    } else {
      tvdbUpdateRunner.runUpdate();
    }

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
        "where ignore_tvdb = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false);

    runUpdateOnResultSet(resultSet);
  }


  private void runUpdateSingle() throws SQLException {
    String singleSeriesTitle = "Unbreakable Kimmy Schmidt"; // update for testing on a single series

    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ? " +
        "and title = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, singleSeriesTitle);

    runUpdateOnResultSet(resultSet);
  }


  private void runUpdateOnResultSet(ResultSet resultSet) throws SQLException {
    debug("Starting update.");

    int i = 0;

    while (resultSet.next()) {
      i++;
      Series series = new Series();

      try {
        processSingleSeries(resultSet, series);
      } catch (Exception e) {
        debug("Show failed on initialization from DB.");
      }

      seriesUpdates++;
      debug(i + " processed.");
    }
  }

  private void processSingleSeries(ResultSet resultSet, Series series) throws SQLException {
    series.initializeFromDBObject(resultSet);

    try {
      updateTVDB(series);
    } catch (Exception e) {
      e.printStackTrace();
      debug("Show failed TVDB: " + series.seriesTitle.getValue());
    }
  }

  private void updateTVDB(Series series) throws SQLException, BadlyFormattedXMLException, ShowFailedException {
    TVDBSeriesUpdater updater = new TVDBSeriesUpdater(connection, series, new NodeReaderImpl());
    updater.updateSeries();

    episodesAdded += updater.getEpisodesAdded();
    episodesUpdated += updater.getEpisodesUpdated();
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

