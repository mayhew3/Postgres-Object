package com.mayhew3.gamesutil.dataobject;

public enum IntegerSize {
  SMALLINT("SMALLINT"),
  INTEGER("INTEGER"),
  BIGINT("BIGINT");

  private final String ddlIdentifier;

  IntegerSize(String sizeIdentifier) {
    this.ddlIdentifier = sizeIdentifier;
  }

  public String getDdlIdentifier() {
    return ddlIdentifier;
  }
}
