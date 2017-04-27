package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.TVDatabaseTest;
import com.mayhew3.gamesutil.dataobject.TiVoLocalProvider;
import com.mayhew3.gamesutil.model.tv.Series;
import com.mayhew3.gamesutil.model.tv.TVDBSeries;
import com.mayhew3.gamesutil.model.tv.TiVoEpisode;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import org.junit.Test;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class TiVoMatchExistingTest extends TVDatabaseTest {
  private TiVoLocalProvider tiVoLocalProvider;
  private String seriesName;

  @Override
  public void setUp() throws URISyntaxException, SQLException {
    super.setUp();
    tiVoLocalProvider = new TiVoLocalProvider(
        "resources\\AtlantaDaylightSavings\\",
        "SeriesListRecordingNow.xml",
        "SeriesDetail.xml");
  }

  @Test
  public void testFirstTiVoEpisodeFoundForExistingSeries() throws SQLException, BadlyFormattedXMLException {
    setupSeries();

    seriesName = "Atlanta";
    List<Series> originalSeriesesWithName = getSeriesWithName(seriesName);
    assertThat(originalSeriesesWithName)
        .as("SANITY: Only one series should exist with name initially.")
        .hasSize(1);

    TiVoCommunicator tiVoCommunicator = new TiVoCommunicator(connection, tiVoLocalProvider);
    tiVoCommunicator.runUpdate(true);

    TiVoLocalProvider notRecording = new TiVoLocalProvider(
        "resources\\AtlantaDaylightSavings\\",
        "SeriesList.xml",
        "SeriesDetail.xml");

    tiVoCommunicator = new TiVoCommunicator(connection, notRecording);
    tiVoCommunicator.runUpdate(true);

    List<Series> afterUpdate = getSeriesWithName(seriesName);
    assertThat(afterUpdate)
        .as("Expect no new series to be added because one already exists with name.")
        .hasSize(1);
  }


  private Series setupSeries() throws SQLException {
    int tvdbSeriesExtId = 234123;

    TVDBSeries tvdbSeries = new TVDBSeries();
    tvdbSeries.initializeForInsert();
    tvdbSeries.tvdbSeriesExtId.changeValue(tvdbSeriesExtId);
    tvdbSeries.commit(connection);

    Series series = new Series();
    series.initializeForInsert();
    series.seriesTitle.changeValue("Atlanta");
    series.addedBy.changeValue("Manual");
    series.tvdbMatchStatus.changeValue("Match Completed");
    series.tvdbSeriesId.changeValue(tvdbSeries.id.getValue());
    series.tvdbSeriesExtId.changeValue(tvdbSeriesExtId);
    series.tivoVersion.changeValue(1);
    series.commit(connection);

    return series;
  }

  private TiVoEpisode getSingleAddedEpisode() throws SQLException {
    List<TiVoEpisode> addedEpisodes = findAddedEpisodes();

    assertThat(addedEpisodes)
        .hasSize(1);

    return addedEpisodes.get(0);
  }

  private List<TiVoEpisode> findAddedEpisodes() throws SQLException {
    List<TiVoEpisode> addedEpisodes = new ArrayList<>();

    String sql = "SELECT * " +
        "FROM tivo_episode " +
        "WHERE program_v2_id = ? " +
        "AND retired = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, "EP0351236842-0362508025", 0);

    while (resultSet.next()) {
      TiVoEpisode tiVoEpisode = new TiVoEpisode();
      tiVoEpisode.initializeFromDBObject(resultSet);

      addedEpisodes.add(tiVoEpisode);
    }

    return addedEpisodes;
  }

  private List<Series> getSeriesWithName(String seriesName) throws SQLException {
    List<Series> serieses = new ArrayList<>();

    String sql = "SELECT * " +
        "FROM series " +
        "WHERE title = ? " +
        "AND retired = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, seriesName, 0);

    while (resultSet.next()) {
      Series series = new Series();
      series.initializeFromDBObject(resultSet);

      serieses.add(series);
    }

    return serieses;
  }

}
