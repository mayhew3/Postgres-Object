package com.mayhew3.gamesutil.dataobject;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.db.PostgresConnectionFactory;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.mayhew3.gamesutil.model.games.Game;
import com.mayhew3.gamesutil.model.games.GameLog;
import com.mayhew3.gamesutil.model.tv.*;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

public class DataSchema {
  List<DataObject> allTables;

  DataSchema(DataObject... dataObjects) {
    allTables = Lists.newArrayList(dataObjects);
  }

  public static DataSchema tv_schema = new DataSchema(
      new ConnectLog(),
      new EdgeTiVoEpisode(),
      new Episode(),
      new ErrorLog(),
      new Genre(),
      new Movie(),
      new PossibleSeriesMatch(),
      new Season(),
      new SeasonViewingLocation(),
      new Series(),
      new SeriesViewingLocation(),
      new TiVoEpisode(),
      new TVDBEpisode(),
      new TVDBSeries(),
      new ViewingLocation()
  );

  public static DataSchema games_schema = new DataSchema(
      new Game(),
      new GameLog()
  );

  public static void main(String... args) throws URISyntaxException, SQLException {
    SQLConnection connection = new PostgresConnectionFactory().createConnection("test");
    tv_schema.validateSchemaAgainstDatabase(connection);
  }

  public void validateSchemaAgainstDatabase(SQLConnection connection) throws SQLException {
    debug("");

    for (DataObject dataObject : allTables) {
      List<String> results = new DataObjectTableValidator(dataObject, connection).matchSchema();

      if (results.isEmpty()) {
        debug("Table " + dataObject.getTableName() + " checks out!");
      } else {
        debug("Issues found for table " + dataObject.getTableName() + ":");
        for (String result : results) {
          debug(" - " + result);
        }
      }

      debug("");
    }
  }


  private static void debug(String s) {
    System.out.println(s);
  }
}
