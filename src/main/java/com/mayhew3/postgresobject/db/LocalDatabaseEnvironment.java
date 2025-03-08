package com.mayhew3.postgresobject.db;

import com.google.common.base.Joiner;
import com.mayhew3.postgresobject.EnvironmentChecker;
import com.mayhew3.postgresobject.exception.MissingEnvException;

import java.util.ArrayList;
import java.util.List;

public class LocalDatabaseEnvironment extends DatabaseEnvironment {

  final String databaseName;
  final Integer port;

  public LocalDatabaseEnvironment(String environmentName, String databaseName, Integer port, Integer pgVersion) {
    super(environmentName, pgVersion);
    this.databaseName = databaseName;
    this.port = port;
  }

  public LocalDatabaseEnvironment(String environmentName, String databaseName, String schemaName, Integer port, Integer pgVersion) {
    super(environmentName, schemaName, pgVersion);
    this.databaseName = databaseName;
    this.port = port;
  }

  @Override
  public String getDatabaseUrl() throws MissingEnvException {
    String localPassword = EnvironmentChecker.getOrThrow("postgres_local_password");

    List<String> params = new ArrayList<>();
    if (this.schemaName != null) {
      params.add("currentSchema=" + this.schemaName);
    }
    params.add("characterEncoding=UTF-8");
    params.add("user=postgres");
    params.add("password=" + localPassword);
    String paramConcatStr = Joiner.on("&").join(params);

    return "jdbc:postgresql://localhost:" + port + "/" + databaseName + "?" + paramConcatStr;
  }

  public String getDatabaseName() {
    return databaseName;
  }


  @Override
  public boolean isLocal() {
    return true;
  }
}
