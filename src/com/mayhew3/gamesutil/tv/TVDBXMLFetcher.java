package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.ArgumentChecker;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.tv.Series;
import com.mayhew3.gamesutil.xml.NodeReader;
import com.mayhew3.gamesutil.xml.NodeReaderImpl;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressWarnings("FieldCanBeLocal")
public class TVDBXMLFetcher {

  private String singleSeriesTitle = "Inside Amy Schumer"; // update for testing on a single series
  private String filePath = "resources\\TVDB_Inside_Amy_Schumer.xml";

  private SQLConnection connection;

  private TVDBXMLFetcher(SQLConnection connection) {
    this.connection = connection;
  }

  public static void main(String... args) throws URISyntaxException, SQLException, IOException, SAXException {
    String identifier = new ArgumentChecker(args).getDBIdentifier();

    SQLConnection connection = new PostgresConnectionFactory().createConnection(identifier);
    TVDBXMLFetcher tvdbxmlFetcher = new TVDBXMLFetcher(connection);

    tvdbxmlFetcher.downloadXMLForSeries();
  }

  private void downloadXMLForSeries() throws SQLException, IOException, SAXException {

    String sql = "select *\n" +
        "from series\n" +
        "where ignore_tvdb = ? " +
        "and title = ? ";
    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql, false, singleSeriesTitle);

    debug("Starting update.");


    if (resultSet.next()) {
      Series series = new Series();

      series.initializeFromDBObject(resultSet);

      NodeReader nodeReader = new NodeReaderImpl(filePath);

      Integer tvdbID = series.tvdbSeriesExtId.getValue();

      String apiKey = System.getenv("TVDB_API_KEY");
      String url = "http://thetvdb.com/api/" + apiKey + "/series/" + tvdbID + "/all/en.xml";

      nodeReader.readXMLFromUrl(url);
    } else {
      throw new IllegalStateException("Series not found: " + singleSeriesTitle);
    }
  }


  protected void debug(Object object) {
    System.out.println(object);
  }

}

