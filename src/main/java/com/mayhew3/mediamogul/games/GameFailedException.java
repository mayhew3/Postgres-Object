package com.mayhew3.mediamogul.games;

public class GameFailedException extends Exception {
  public GameFailedException(String errorMessage) {
    super(errorMessage);
  }
}
