package com.mayhew3.mediamogul.tv.exception;

public class MultipleMatchesException extends ShowFailedException {
  public MultipleMatchesException(String errorMessage) {
    super(errorMessage);
  }
}
