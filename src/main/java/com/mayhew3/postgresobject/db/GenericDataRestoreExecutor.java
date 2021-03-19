package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.exception.MissingEnvException;
import org.joda.time.DateTime;

import java.io.IOException;

public class GenericDataRestoreExecutor {

  public static void main(String[] args) throws MissingEnvException, IOException, InterruptedException {
    DateTime aMonthAgo = new DateTime(2020, 1, 5, 0, 0, 0);
    DataRestoreLocalExecutor executor = new DataRestoreLocalExecutor(InternalDatabaseEnvironments.test, InternalDatabaseEnvironments.test, "PostgresObject", aMonthAgo);
    executor.runUpdate();
  }
}
