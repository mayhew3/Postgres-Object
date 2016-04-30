package com.mayhew3.gamesutil.dataobject;

public enum Nullability {
  NOT_NULL(false),
  NULLABLE(true);

  private final Boolean allowNulls;

  Nullability(Boolean allowNulls) {
    this.allowNulls = allowNulls;
  }

  public Boolean getAllowNulls() {
    return allowNulls;
  }
}
