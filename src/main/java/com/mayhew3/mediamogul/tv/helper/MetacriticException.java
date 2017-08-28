package com.mayhew3.mediamogul.tv.helper;

import com.mayhew3.mediamogul.tv.exception.ShowFailedException;

public class MetacriticException extends ShowFailedException {
  public MetacriticException(String errorMessage) {
    super(errorMessage);
  }
}
