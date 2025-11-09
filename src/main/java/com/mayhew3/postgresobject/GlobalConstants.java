package com.mayhew3.postgresobject;

public class GlobalConstants {
  public static String appLabel = "Test";
  public static String schemaName = "test";

  public static String schemaNameSoftball = "softball";

  // Note: postgresVersion is deprecated and no longer used for port calculation.
  // Use POSTGRES_VERSION environment variable in InternalDatabaseEnvironments instead.
  public static Integer postgresVersion = 14;
}
