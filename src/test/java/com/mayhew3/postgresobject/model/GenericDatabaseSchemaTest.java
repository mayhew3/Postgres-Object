package com.mayhew3.postgresobject.model;

import com.mayhew3.postgresobject.dataobject.DataSchema;
import com.mayhew3.postgresobject.dataobject.DataSchemaMock;
import com.mayhew3.postgresobject.db.DatabaseEnvironment;
import com.mayhew3.postgresobject.db.InternalDatabaseEnvironments;

public class GenericDatabaseSchemaTest extends PostgresSchemaTest {
  @Override
  public DataSchema getDataSchema() {
    return DataSchemaMock.schemaDef;
  }

  @Override
  public DatabaseEnvironment getDatabaseEnvironment() {
    return InternalDatabaseEnvironments.test;
  }
}
