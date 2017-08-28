package com.mayhew3.mediamogul.tv.exception;

public class MalformedTVDBEpisodeException extends ShowFailedException {
  public MalformedTVDBEpisodeException(String errorMessage) {
    super(errorMessage);
  }
}
