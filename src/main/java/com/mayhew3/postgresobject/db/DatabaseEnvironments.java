package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;

public class DatabaseEnvironments {
  public static DatabaseEnvironment test = new DatabaseEnvironment("test", 13) {
    @Override
    public String getDatabaseUrl() throws MissingEnvException {
      String localPassword = EnvironmentChecker.getOrThrow("postgres_local_password");
      int port = 5432 - 9 + getPgVersion();
      return "jdbc:postgresql://localhost:" + port + "/pg_object_test?user=postgres&password=" + localPassword;
    }

    @Override
    public boolean isLocal() {
      return true;
    }
  };
}
