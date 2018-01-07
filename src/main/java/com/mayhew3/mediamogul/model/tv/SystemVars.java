package com.mayhew3.mediamogul.model.tv;

import com.mayhew3.mediamogul.dataobject.DataObject;
import com.mayhew3.mediamogul.dataobject.FieldValueBoolean;
import com.mayhew3.mediamogul.dataobject.FieldValueInteger;
import com.mayhew3.mediamogul.dataobject.Nullability;
import com.mayhew3.mediamogul.db.SQLConnection;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SystemVars extends DataObject {

  public FieldValueInteger ratingYear = registerIntegerField("rating_year", Nullability.NOT_NULL);
  public FieldValueBoolean ratingLocked = registerBooleanField("rating_locked", Nullability.NOT_NULL);

  @Override
  public String getTableName() {
    return "system_vars";
  }

  public static SystemVars getSystemVars(SQLConnection connection) throws SQLException {
    String sql = "SELECT * " +
        "FROM system_vars";

    ResultSet resultSet = connection.prepareAndExecuteStatementFetch(sql);
    if (resultSet.next()) {
      SystemVars systemVars = new SystemVars();
      systemVars.initializeFromDBObject(resultSet);

      return systemVars;
    } else {
      throw new IllegalStateException("No rows found in system_vars.");
    }
  }
}
