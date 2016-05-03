package com.mayhew3.gamesutil.dataobject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.db.SQLConnection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataSchema {
  private List<DataObject> allTables;

  public DataSchema(DataObject... dataObjects) {
    allTables = Lists.newArrayList(dataObjects);
  }

  public List<DataObjectMismatch> validateSchemaAgainstDatabase(SQLConnection connection) throws SQLException {
    List<DataObjectMismatch> allResults = new ArrayList<>();
    for (DataObject dataObject : allTables) {
      allResults.addAll(new DataObjectTableValidator(dataObject, connection).matchSchema());
    }
    return allResults;
  }

  public List<DataObject> getAllTables() {
    return ImmutableList.copyOf(allTables);
  }
}
