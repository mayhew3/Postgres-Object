package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.SQLConnection;

public enum IntegerSize {
  SMALLINT("SMALLINT", "SMALLINT"),
  INTEGER("INTEGER", "INT"),
  BIGINT("BIGINT", "BIGINT");

  private final String postgresIdentifier;
  private final String mySqlIdentifier;

  IntegerSize(String sizeIdentifier, String mySqlIdentifier) {
    this.postgresIdentifier = sizeIdentifier;
    this.mySqlIdentifier = mySqlIdentifier;
  }

  public String getDdlIdentifier(SQLConnection connection) {
    if (connection.getDatabaseType() == SQLConnection.DatabaseType.POSTGRES) {
      return postgresIdentifier;
    } else {
      return mySqlIdentifier;
    }
  }
}
