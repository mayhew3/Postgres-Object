package com.mayhew3.postgresobject;

public class GlobalConstants {
  public static String appLabel = "Test";

  // Get schema name from environment variable POSTGRES_SCHEMA, or use default "test"
  // This allows different environments to use different schema names
  public static String schemaName = getSchemaName();

  public static String schemaNameSoftball = "softball";

  // Note: postgresVersion is deprecated and no longer used for port calculation.
  // Use POSTGRES_VERSION environment variable in InternalDatabaseEnvironments instead.
  public static Integer postgresVersion = 14;

  /**
   * Get PostgreSQL schema name from environment variable POSTGRES_SCHEMA, or use default "test".
   */
  private static String getSchemaName() {
    String schemaEnv = System.getenv("POSTGRES_SCHEMA");
    return schemaEnv != null ? schemaEnv : "test";
  }
}
