package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;

public class DatabaseEnvironments {
  public static LocalDatabaseEnvironment test = new LocalDatabaseEnvironment("test", "pg_object_test", 5436, 13);
}
