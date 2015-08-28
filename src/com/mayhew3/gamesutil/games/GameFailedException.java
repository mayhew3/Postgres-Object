package com.mayhew3.gamesutil.games;

public class GameFailedException extends Exception {
  public GameFailedException(String errorMessage) {
    super(errorMessage);
  }
}
