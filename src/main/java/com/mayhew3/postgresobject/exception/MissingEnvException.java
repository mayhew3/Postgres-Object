package com.mayhew3.postgresobject.exception;

public class MissingEnvException extends Exception {
  public MissingEnvException(String errorMessage) {
    super(errorMessage);
  }
}
