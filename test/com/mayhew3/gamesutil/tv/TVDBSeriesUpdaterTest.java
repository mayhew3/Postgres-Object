package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.TVDatabaseTest;
import com.mayhew3.gamesutil.model.tv.Episode;
import com.mayhew3.gamesutil.model.tv.Series;
import com.mayhew3.gamesutil.model.tv.TVDBEpisode;
import com.mayhew3.gamesutil.model.tv.TVDBSeries;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.NodeReaderImpl;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.fest.assertions.Assertions.assertThat;

public class TVDBSeriesUpdaterTest extends TVDatabaseTest {

  @Test
  public void testIDChangedForTVDBEpisode() throws SQLException, ShowFailedException, BadlyFormattedXMLException {
    String seriesName = "Inside Amy Schumer";

    createSeries(seriesName, 265374);

    Series series = findSeriesWithTitle(seriesName);

    // fake ID - use so that XML will find a different ID and change it and not add a new episode with same episode number.
    Integer originalID = 5;
    Integer expectedID = 5580497;

    addEpisode(series, 4, 1, "The World's Most Interesting Woman in the World", 5578415);
    addEpisode(series, 4, 2, "Welcome to the Gun Show", originalID);
    addEpisode(series, 4, 3, "Brave", 5552985);

    NodeReaderImpl nodeReader = new NodeReaderImpl();

    TVDBLocalFileProvider provider = new TVDBLocalFileProvider("resources\\test_input_renumbering.xml");

    TVDBSeriesUpdater tvdbSeriesUpdater = new TVDBSeriesUpdater(connection, series, nodeReader, provider);
    tvdbSeriesUpdater.updateSeries();

    TVDBEpisode retiredEpisode = findTVDBEpisodeWithTVDBID(originalID);
    assertThat(retiredEpisode)
        .isNull();

    TVDBEpisode updatedEpisode = findTVDBEpisodeWithTVDBID(expectedID);
    assertThat(updatedEpisode)
        .isNotNull();
    assertThat(updatedEpisode.retired.getValue())
        .isEqualTo(0);
    assertThat(updatedEpisode.seasonNumber.getValue())
        .isEqualTo(4);
    assertThat(updatedEpisode.episodeNumber.getValue())
        .isEqualTo(2);
  }

  @Test
  public void testEpisodeNumbersSwapped() throws SQLException, IOException, SAXException, ShowFailedException, BadlyFormattedXMLException {
    String seriesName = "Inside Amy Schumer";

    createSeries(seriesName, 265374);

    Series series = findSeriesWithTitle(seriesName);

    int tvdbId_1 = 5578415;
    int tvdbId_2 = 5552985;
    int tvdbId_3 = 5580497;

    addEpisode(series, 4, 1, "The World's Most Interesting Woman in the World", tvdbId_1);
    addEpisode(series, 4, 2, "Brave", tvdbId_2);
    addEpisode(series, 4, 3, "Welcome to the Gun Show", tvdbId_3);

    NodeReaderImpl nodeReader = new NodeReaderImpl();

    TVDBLocalFileProvider provider = new TVDBLocalFileProvider("resources\\test_input_renumbering.xml");

    TVDBSeriesUpdater tvdbSeriesUpdater = new TVDBSeriesUpdater(connection, series, nodeReader, provider);
    tvdbSeriesUpdater.updateSeries();

    TVDBEpisode tvdbEpisode = findTVDBEpisodeWithTVDBID(tvdbId_2);

    assertThat(tvdbEpisode)
        .isNotNull();
    assertThat(tvdbEpisode.episodeNumber.getValue())
        .isEqualTo(3);

    TVDBEpisode thirdEpisode = findTVDBEpisodeWithTVDBID(tvdbId_3);

    assertThat(thirdEpisode)
        .isNotNull();
    assertThat(thirdEpisode.episodeNumber.getValue())
        .isEqualTo(2);
  }



  // private methods

  private Series createSeries(String seriesName, Integer tvdbId) throws SQLException {
    TVDBSeries tvdbSeries = new TVDBSeries();
    tvdbSeries.initializeForInsert();
    tvdbSeries.tvdbId.changeValue(tvdbId);
    tvdbSeries.name.changeValue(seriesName);
    tvdbSeries.commit(connection);

    Series series = new Series();
    series.initializeForInsert();
    series.seriesTitle.changeValue(seriesName);
    series.tvdbId.changeValue(tvdbId);
    series.tvdbSeriesId.changeValue(tvdbSeries.id.getValue());
    series.commit(connection);

    return series;
  }

  private void addEpisode(Series series, Integer seasonNumber, Integer episodeNumber, String episodeTitle, Integer tvdbEpisodeId) throws SQLException {
    TVDBEpisode tvdbEpisode = new TVDBEpisode();
    tvdbEpisode.initializeForInsert();
    tvdbEpisode.tvdbSeriesId.changeValue(series.tvdbSeriesId.getValue());
    tvdbEpisode.seriesName.changeValue(series.seriesTitle.getValue());
    tvdbEpisode.seasonNumber.changeValue(seasonNumber);
    tvdbEpisode.episodeNumber.changeValue(episodeNumber);
    tvdbEpisode.name.changeValue(episodeTitle);
    tvdbEpisode.tvdbId.changeValue(tvdbEpisodeId);
    tvdbEpisode.commit(connection);

    Episode episode = new Episode();
    episode.initializeForInsert();
    episode.tvdbEpisodeId.changeValue(tvdbEpisode.id.getValue());
    episode.seriesTitle.changeValue(series.seriesTitle.getValue());
    episode.setSeason(seasonNumber);
    episode.seasonEpisodeNumber.changeValue(episodeNumber);
    episode.title.changeValue(episodeTitle);
    episode.seriesId.changeValue(series.id.getValue());
    episode.commit(connection);
  }

  private Series findSeriesWithTitle(String title) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM series " +
            "WHERE title = ?", title
    );
    if (resultSet.next()) {
      Series series = new Series();
      series.initializeFromDBObject(resultSet);
      return series;
    } else {
      return null;
    }
  }
  private TVDBEpisode findTVDBEpisodeWithTVDBID(Integer tvdbId) throws SQLException {
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * " +
            "FROM tvdb_episode " +
            "WHERE tvdb_id = ?", tvdbId
    );
    if (resultSet.next()) {
      TVDBEpisode tvdbEpisode = new TVDBEpisode();
      tvdbEpisode.initializeFromDBObject(resultSet);
      return tvdbEpisode;
    } else {
      return null;
    }
  }
}