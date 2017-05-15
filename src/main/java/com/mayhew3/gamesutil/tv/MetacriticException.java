package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.tv.exception.ShowFailedException;

public class MetacriticException extends ShowFailedException {
  public MetacriticException(String errorMessage) {
    super(errorMessage);
  }
}