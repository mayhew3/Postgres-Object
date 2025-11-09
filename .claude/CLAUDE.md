# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**postgres-object** is a Java ORM library that provides an object-relational mapping layer for PostgreSQL and MySQL databases. It allows developers to define database schemas and objects in code and automatically handles schema validation, CRUD operations, and database migrations.

## Build System & Commands

This project uses Gradle with Java 21 as the build system.

### Common Commands

```bash
# Build the project
./gradlew build

# Run all tests locally (including integration tests)
# IMPORTANT: Use the test script instead of direct gradlew commands
# to avoid having to manually set environment variables
test-local.bat        # Windows
./test-local.sh       # Linux/Mac

# Run a specific test class (with local schema)
./test-local.sh --tests com.mayhew3.postgresobject.dataobject.PostgreSQLCRUDIntegrationTest

# Run a single test method
./test-local.sh --tests com.mayhew3.postgresobject.dataobject.DataObjectTest.testSimpleInsert

# Check for dependency updates
./gradlew dependencyUpdates

# Install to local Maven repository
./gradlew install
```

**Note**: The `test-local` scripts automatically set `POSTGRES_SCHEMA=postgres_object_test` to use the local test schema. This prevents requiring permission for environment variable commands.

## Architecture

### Core Components

1. **DataObject** (`src/main/java/com/mayhew3/postgresobject/dataobject/DataObject.java`)
   - Base class for all database table representations
   - Handles INSERT/UPDATE mode tracking
   - Manages field values, foreign keys, unique constraints, and indices
   - All DataObjects must have an `id` field (FieldValueSerial)
   - Provides `initializeFromDBObject()` for loading from ResultSet and `initializeForInsert()` for new records
   - Tracks changed fields for efficient updates

2. **FieldValue** and Field Types
   - Abstract base: `FieldValue`
   - Concrete types: `FieldValueString`, `FieldValueInteger`, `FieldValueBigDecimal`, `FieldValueBoolean`, `FieldValueTimestamp`, `FieldValueDate`, `FieldValueSerial`, `FieldValueForeignKey`
   - Each FieldValue tracks its own changed state, nullability, and DB-specific type conversions
   - FieldConversion classes handle database-specific SQL type mappings

3. **SQLConnection** Interface (`src/main/java/com/mayhew3/postgresobject/db/SQLConnection.java`)
   - Abstraction over database connections (PostgreSQL and MySQL)
   - Provides prepared statement execution methods
   - Implementations: `PostgresConnection`, `MySQLConnection`

4. **DataSchema** (`src/main/java/com/mayhew3/postgresobject/dataobject/DataSchema.java`)
   - Container for multiple DataObject definitions
   - Validates schema against live database via `validateSchemaAgainstDatabase()`
   - Returns list of `DataObjectMismatch` objects for any discrepancies

5. **DatabaseEnvironment** (`src/main/java/com/mayhew3/postgresobject/db/DatabaseEnvironment.java`)
   - Abstract base for database configuration
   - Implementations: `LocalDatabaseEnvironment`, `RemoteDatabaseEnvironment`, `HerokuDatabaseEnvironment`, `InternalDatabaseEnvironments`
   - Provides database URL, schema name, and PostgreSQL version

6. **DataObjectTableValidator**
   - Validates DataObject definitions against actual database tables
   - Checks field types, constraints, foreign keys, and indices
   - Used internally by DataSchema validation

7. **DatabaseRecreator**
   - Drops and recreates database schema based on DataSchema definition
   - Used primarily in tests to ensure clean database state

### Data Backup/Restore Tools

The library includes utilities for backup and restore operations:
- `DataBackupExecutor` / `DataRestoreExecutor` interfaces
- Local and remote executors for data backup/restore
- Schema-specific backup executors

### Package Structure

```
com.mayhew3.postgresobject/
├── dataobject/           # Core ORM classes (DataObject, FieldValue, DataSchema)
├── db/                   # Database connection and environment classes
└── exception/            # Custom exceptions (MissingEnvException)
```

## Testing

### Test Structure

Tests are organized under `src/test/java/com/mayhew3/postgresobject/`:
- `dataobject/` - Tests for DataObject, FieldValue types, validation
- `db/` - Connection and database-specific tests
- `model/` - Schema validation tests (PostgresSchemaTest, MySQLSchemaTest)

### DatabaseTest Base Class

All database-dependent tests extend `DatabaseTest`, which:
- Provides a `SQLConnection connection` field
- Uses `@Before` to recreate a clean test database before each test
- Requires subclasses to implement `getTestEnvironment()` to provide test DB configuration

### Environment Variables

Tests may require database connection environment variables. Check specific test classes for required environment configuration.

## Key Patterns

### Creating a DataObject

```java
public class MyTable extends DataObject {
    public FieldValueString name = registerStringField("name", 100);
    public FieldValueInteger age = registerIntegerField("age", Nullability.NOT_NULL);

    @Override
    public String getTableName() {
        return "my_table";
    }
}
```

### Working with DataObjects

- **Insert mode**: Call `initializeForInsert()`, set field values, then `commit(connection)`
- **Update mode**: Initialize from ResultSet via `initializeFromDBObject(resultSet)`, modify fields, then `commit(connection)`
- The framework automatically tracks which mode the object is in and generates appropriate SQL

### Schema Validation

```java
DataSchema schema = new DataSchema(new MyTable(), new OtherTable());
List<DataObjectMismatch> mismatches = schema.validateSchemaAgainstDatabase(connection);
```

## Dependencies

Key dependencies (from build.gradle):
- PostgreSQL JDBC driver: `org.postgresql:postgresql:42.2.2`
- MySQL Connector: `mysql:mysql-connector-java:5.1.22`
- Guava: `com.google.guava:guava:27.0.1-jre`
- Joda-Time: `joda-time:joda-time:2.9.9`
- Log4j2: `org.apache.logging.log4j:log4j-core:2.11.2`
- JUnit 4 for testing
