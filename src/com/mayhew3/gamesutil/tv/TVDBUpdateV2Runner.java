package com.mayhew3.gamesutil.tv;

import com.google.common.collect.Lists;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.Series;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.JSONReader;
import com.mayhew3.gamesutil.xml.JSONReaderImpl;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static com.sun.xml.internal.bind.api.impl.NameConverter.smart;

public class TVDBUpdateV2Runner {

  private Integer seriesUpdates = 0;
  private Integer episodesAdded = 0;
  private Integer episodesUpdated = 0;

  private SQLConnection connection;

  private TVDBJWTProvider tvdbjwtProvider;
  private JSONReader jsonReader;

  TVDBUpdateV2Runner(SQLConnection connection, TVDBJWTProvider tvdbjwtProvider, JSONReader jsonReader) {
    this.connection = connection;
    this.tvdbjwtProvider = tvdbjwtProvider;
    this.jsonReader = jsonReader;
  }

  public static void main(String... args) throws URISyntaxException, SQLException, UnirestException {
    List<String> argList = Lists.newArrayList(args);
    Boolean singleSeries = argList.contains("SingleSeries");
    Boolean quickMode = argList.contains("Quick");
    Boolean smartMode = argList.contains("Smart");
    String identifier = new ArgumentChecker(args).getDBIdentifier();

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);
    TVDBUpdateV2Runner tvdbUpdateRunner = new TVDBUpdateV2Runner(connection, new TVDBJWTProviderImpl(), new JSONReaderImpl());

    if (singleSeries) {
      tvdbUpdateRunner.runUpdateSingle();
    } else if (quickMode) {
      tvdbUpdateRunner.runQuickUpdate();
    } else if (smartMode) {
      tvdbUpdateRunner.runSmartUpdate();
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

  /**
   * Go to theTVDB and update new series.
   *
   * @throws SQLException if query to get series to update fails. Any one series update will not halt operation of the
   *                    script, but if the query to find all the serieses fails, the operation can't continue.
   */
  private void runQuickUpdate() throws SQLException {
    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ? " +
        "and (tvdb_new = ? or needs_tvdb_redo = ? or matched_wrong = ?) ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, true, true, true);

    runUpdateOnResultSet(resultSet);
  }



  private void runUpdateSingle() throws SQLException {
    String singleSeriesTitle = "Halt and Catch Fire"; // update for testing on a single series

    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ? " +
        "and title = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, singleSeriesTitle);

    runUpdateOnResultSet(resultSet);
  }

  private void runSmartUpdate() throws SQLException, UnirestException {
    DateTime now = new DateTime(new Date());
    DateTime anHourAgo = now.minusHours(1);

    JSONObject updatedSeries = tvdbjwtProvider.getUpdatedSeries(anHourAgo);
    @NotNull JSONArray seriesArray = jsonReader.getArrayWithKey(updatedSeries, "data");

    for (int i = 0; i < seriesArray.length(); i++) {
      JSONObject seriesRow = seriesArray.getJSONObject(i);
      @NotNull Integer seriesId = jsonReader.getIntegerWithKey(seriesRow, "id");

      String sql = "select * " +
          "from series " +
          "where ignore_tvdb = ? " +
          "and tvdb_series_ext_id = ?";

      @NotNull ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, seriesId);
      if (resultSet.next()) {
        Series series = new Series();

        try {
          processSingleSeries(resultSet, series);
        } catch (Exception e) {
          debug("Show failed on initialization from DB.");
        }
      }
    }
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

  private void updateTVDB(Series series) throws SQLException, BadlyFormattedXMLException, ShowFailedException, UnirestException {
    TVDBSeriesV2Updater updater = new TVDBSeriesV2Updater(connection, series, tvdbjwtProvider, jsonReader);
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

