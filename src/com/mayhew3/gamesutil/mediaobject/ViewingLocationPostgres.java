package com.mayhew3.gamesutil.mediaobject;

import com.mayhew3.gamesutil.games.PostgresConnection;
import com.sun.istack.internal.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ViewingLocationPostgres extends MediaObjectPostgreSQL {

  /* Data */
  public FieldValueString viewingLocationName = registerStringField("name");

  @Override
  protected String getTableName() {
    return "viewing_location";
  }

  @Override
  public String toString() {
    return viewingLocationName.getValue();
  }


  @NotNull
  public static ViewingLocationPostgres findOrCreate(PostgresConnection connection, String viewingLocationName) throws SQLException {
    ViewingLocationPostgres viewingLocationPostgres = new ViewingLocationPostgres();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * FROM " + viewingLocationPostgres.getTableName() + " " +
            "WHERE " + viewingLocationPostgres.viewingLocationName.getFieldName() + " = ?",
        viewingLocationName);

    if (connection.hasMoreElements(resultSet)) {
      viewingLocationPostgres.initializeFromDBObject(resultSet);
    } else {
      viewingLocationPostgres.initializeForInsert();
      viewingLocationPostgres.viewingLocationName.changeValue(viewingLocationName);
      viewingLocationPostgres.commit(connection);
    }

    return viewingLocationPostgres;
  }
}
