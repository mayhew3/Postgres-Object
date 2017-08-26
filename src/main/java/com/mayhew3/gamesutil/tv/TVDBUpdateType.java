package com.mayhew3.gamesutil.tv;

public enum TVDBUpdateType {
  FULL("Full"),
  SMART("Smart"),
  FEW_ERRORS("FewErrors"),
  OLD_ERRORS("OldErrors"),
  SINGLE("Single"),
  RECENT("Recent"),
  QUICK("Quick"),
  AIRTIMES("AirTimes"),
  SANITY("Sanity"),
  SERVICE("Service"),
  EPISODE_MATCH("EpisodeMatch"),
  FIRST_PASS("FirstPass");

  private final String typekey;

  TVDBUpdateType(String dbString) {
    this.typekey = dbString;
  }

  public String getTypekey() {
    return typekey;
  }
}
