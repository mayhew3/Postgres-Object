package com.mayhew3.gamesutil;

import com.google.common.collect.Lists;

import java.util.List;

public class ArgumentChecker {
  private List<String> argList;

  public ArgumentChecker(String... args) {
    this.argList = Lists.newArrayList(args);
  }

  public String getDBIdentifier() {
    if (argList.contains("Test")) {
      return "test";
    }
    if (argList.contains("Local")) {
      return "local";
    }
    if (argList.contains("Heroku")) {
      return "heroku";
    }
    throw new IllegalArgumentException("Need to specify either Test or Local or Heroku in arguments.");
  }
}
