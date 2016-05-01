package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.DataObject;
import com.mayhew3.gamesutil.dataobject.FieldValueString;
import com.mayhew3.gamesutil.dataobject.IntegerSize;
import com.mayhew3.gamesutil.dataobject.Nullability;
import com.mayhew3.gamesutil.db.SQLConnection;
import com.sun.istack.internal.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Genre extends DataObject {

  /* Data */
  public FieldValueString genreName = registerStringField("name", Nullability.NOT_NULL);

  public Genre() {
    changeIdIntegerSize(IntegerSize.SMALLINT);
  }

  @Override
  protected String getTableName() {
    return "genre";
  }

  @Override
  public String toString() {
    return genreName.getValue();
  }

  @NotNull
  public static Genre findOrCreate(SQLConnection connection, String genreName) throws SQLException {
    Genre genre = new Genre();

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(
        "SELECT * FROM genre WHERE " + genre.genreName.getFieldName() + " = ?",
        genreName);

    if (resultSet.next()) {
      genre.initializeFromDBObject(resultSet);
    } else {
      genre.initializeForInsert();
      genre.genreName.changeValue(genreName);
      genre.commit(connection);
    }

    return genre;
  }
}
