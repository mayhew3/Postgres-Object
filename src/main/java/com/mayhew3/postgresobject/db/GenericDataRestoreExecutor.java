package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.exception.MissingEnvException;

import java.io.IOException;

public class GenericDataRestoreExecutor {

  public static void main(String[] args) throws MissingEnvException, IOException, InterruptedException {
    DataRestoreLocalExecutor executor = new DataRestoreLocalExecutor(
        "local",
        "local",
        11,
        "OscarsAngular",
        "oscars");
    executor.runUpdate();
  }
}
