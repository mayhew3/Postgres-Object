package com.mayhew3.gamesutil.tv.helper;

import com.google.common.collect.Lists;

import java.util.Optional;

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

  public static Optional<TVDBUpdateType> getUpdateType(final String typekey) {
    return Lists.newArrayList(TVDBUpdateType.values())
        .stream()
        .filter(tvdbUpdateType -> tvdbUpdateType.typekey.equalsIgnoreCase(typekey))
        .findAny();
  }

  @Override
  public String toString() {
    return getTypekey();
  }
}
