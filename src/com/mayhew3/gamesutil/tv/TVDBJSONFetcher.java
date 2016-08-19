package com.mayhew3.gamesutil.tv;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.Series;
import javafx.util.Pair;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class TVDBJSONFetcher {

  private String singleSeriesTitle = "Inside Amy Schumer"; // update for testing on a single series

  private SQLConnection connection;

  private TVDBJSONFetcher(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String... args) throws URISyntaxException, SQLException, IOException, UnirestException {
    String identifier = new ArgumentChecker(args).getDBIdentifier();

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);
    TVDBJSONFetcher tvdbJsonFetcher = new TVDBJSONFetcher(connection);

    tvdbJsonFetcher.downloadJSONForSeries();
  }

  private void downloadJSONForSeries() throws SQLException, IOException, UnirestException {

    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ? " +
        "and title = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, singleSeriesTitle);

    debug("Starting update.");


    if (resultSet.next()) {
      Series series = new Series();

      series.initializeFromDBObject(resultSet);
      Integer tvdbSeriesId = series.tvdbSeriesExtId.getValue();


      TVDBJWTProviderImpl tvdbjwtProvider = new TVDBJWTProviderImpl();

      tvdbjwtProvider.writeSearchToFile(singleSeriesTitle);
      tvdbjwtProvider.writeSeriesToFile(tvdbSeriesId);

      // NOTE: After every run, need to truncate the _episodes.json file to only include the episodes below.
      // todo: figure out a way to update it automatically? Maybe use the query to get each episode and manually
      // todo: concatenate each data json into a json array? Or can I manipulate the JSON object directly,
      // todo: and remove the episodes I don't want?
//      tvdbjwtProvider.writeEpisodeSummariesToFile(tvdbSeriesId);

      List<Pair<Integer, Integer>> episodeNumbers = new ArrayList<>();
      episodeNumbers.add(new Pair<>(4, 1));
      episodeNumbers.add(new Pair<>(4, 2));
      episodeNumbers.add(new Pair<>(4, 3));
      tvdbjwtProvider.writeEpisodeDetailsToFiles(tvdbSeriesId, episodeNumbers);

      tvdbjwtProvider.writePostersToFile(tvdbSeriesId);

    } else {
      throw new IllegalStateException("Series not found: " + singleSeriesTitle);
    }
  }


  protected void debug(Object object) {
    System.out.println(object);
  }

}

