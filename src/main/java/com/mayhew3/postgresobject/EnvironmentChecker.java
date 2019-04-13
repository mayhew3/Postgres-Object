package com.mayhew3.postgresobject;

import com.mayhew3.postgresobject.exception.MissingEnvException;

public class EnvironmentChecker {
  public static String getOrThrow(String envLabel) throws MissingEnvException {
    String envValue = System.getenv(envLabel);
    if (envValue == null) {
      throw new MissingEnvException("No environment variable found for '" + envLabel + "'!");
    }

    return envValue;
  }
}
