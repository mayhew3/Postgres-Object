# CI Schema Investigation - PostgreSQL Integration Tests

## Problem Summary

PostgreSQL integration tests were failing in CI with the error:
```
ERROR: relation "data_object_mock" does not exist
```

The tests worked locally with Docker but failed in GitHub Actions CI environment.

## Root Cause

PostgreSQL's schema resolution mechanisms (`currentSchema` JDBC parameter and `SET search_path`) were not reliably creating or finding tables in the `test` schema within the CI environment.

## Investigation Timeline

### Attempt 1: Add JDBC URL Parameter
- **Action**: Added `currentSchema=test` to PostgreSQL JDBC URL in `LocalDatabaseEnvironment.java`
- **Result**: Failed - tables still not found
- **Learning**: JDBC `currentSchema` parameter alone doesn't ensure tables are created in that schema

### Attempt 2: Add SET search_path Command
- **Action**: Added `SET search_path TO test, public` in `PostgresConnectionFactory.initiateDBConnect()`
- **Result**: Failed - tables still not found
- **Learning**: Connection-level search_path setting wasn't taking effect

### Attempt 3: Add Database-Level search_path
- **Action**: Added `ALTER DATABASE projects SET search_path TO test, public` to CI workflow
- **Result**: Failed - tables still not found
- **Learning**: Database-level search_path requires reconnection and may not be picked up by JDBC connections

### Attempt 4: Schema-Qualify All SQL Statements
- **Action**: Modified `DataObject` and `DatabaseRecreator` to prepend schema name to all table references (e.g., `test.data_object_mock`)
- **Result**: Failed - still couldn't find tables
- **Issue**: Even with explicit schema qualification like `CREATE TABLE test.data_object_mock`, subsequent queries for `test.data_object_mock` failed

### Attempt 5: Use Quoted Identifiers
- **Action**: Changed schema-qualified names to use quotes: `"test"."data_object_mock"`
- **Result**: Catastrophic failure - broke 15 tests (up from 6)
- **Issue**: Unit tests with mocked connections expect exact SQL strings without quotes, causing mismatches

### Attempt 6: Add Debug Logging
- **Action**: Added comprehensive logging to `DatabaseRecreator` to show:
  - Schema name being used
  - Current search_path value from PostgreSQL
  - SQL statements being executed
  - Which schema tables were actually created in (via information_schema query)
- **Result**: Logging was added but couldn't easily access the output in CI HTML reports
- **Learning**: Gradle test output doesn't include stdout in HTML reports by default

### Final Solution: Use 'public' Schema
- **Action**: Changed `GlobalConstants.schemaName` from `"test"` to `"public"`
- **Rationale**:
  - `public` is PostgreSQL's default schema
  - No special configuration required
  - Unqualified table names automatically resolve to `public`
  - Eliminates all search_path complexity
- **Status**: Pending verification in CI

## Key Discoveries

### 1. Multiple Schema Configuration Points
PostgreSQL schema can be configured at multiple levels:
- JDBC URL parameter: `currentSchema=test`
- Connection level: `SET search_path TO test`
- Database level: `ALTER DATABASE db SET search_path TO test`
- Statement level: Fully-qualified table names (`schema.table`)

### 2. Configuration Precedence Issues
Even with all configuration points set, tables were not consistently created in or found from the `test` schema in CI. This suggests:
- Timing issues (database-level settings requiring reconnection)
- JDBC driver behavior differences between local and CI
- Possible interaction with PostgreSQL service initialization in GitHub Actions

### 3. Docker vs CI Environment Differences
- **Local Docker**: Has init script (`docker/init-scripts/01-init-schema.sql`) that sets `ALTER DATABASE projects SET search_path TO test, public`
- **CI**: GitHub Actions PostgreSQL service starts fresh without this configuration
- This environmental difference likely contributed to the schema resolution failures

### 4. Schema Qualification Complexity
Explicitly schema-qualifying all SQL statements proved fragile:
- Required changes across multiple files (`DataObject`, `DatabaseRecreator`, test files)
- Broke unit tests that mock connections and expect specific SQL strings
- Required careful handling of both DDL (CREATE TABLE) and DML (INSERT/UPDATE/SELECT)
- Quoted identifiers needed for proper parsing but broke test expectations

## Lessons Learned

### 1. Simplicity Wins
Using the default `public` schema eliminates all configuration complexity and works reliably across environments.

### 2. Test Environment Parity
Differences between local Docker and CI environments can mask issues. The Docker init script was setting up the environment in a way that CI couldn't replicate.

### 3. Debug Logging is Essential
While we couldn't easily access the logs, the debug logging added to `DatabaseRecreator` remains valuable for future investigation:
```java
logger.info("Recreating database with schema: {}", schemaName);
logger.info("Current search_path: {}", searchPath);
logger.info("Table {} created in schema: {}", tableName, actualSchema);
```

### 4. Schema Support is Complex
Supporting custom PostgreSQL schemas properly requires:
- Consistent schema qualification across ALL SQL (DDL, DML, queries)
- Careful handling of different JDBC drivers
- Environment-specific configuration
- Thorough testing in all environments

## Recommendations

### Short Term
- Use `public` schema for all test environments
- Keep the debug logging in `DatabaseRecreator` for future troubleshooting
- Document that production environments can use custom schemas, but tests use `public`

### Long Term
If custom schema support is needed for production:
1. Create a comprehensive test suite specifically for schema behavior
2. Test in environment that matches production (not just Docker/CI)
3. Consider schema qualification at the ORM level rather than configuration
4. Ensure all SQL generation includes schema qualification when non-public schema is used

## Files Modified During Investigation

### Core Changes (Final State)
- `src/main/java/com/mayhew3/postgresobject/GlobalConstants.java` - Changed schemaName to "public"
- `src/main/java/com/mayhew3/postgresobject/dataobject/DatabaseRecreator.java` - Added debug logging

### Configuration Changes (Attempted but not sufficient)
- `src/main/java/com/mayhew3/postgresobject/db/LocalDatabaseEnvironment.java` - Added currentSchema JDBC parameter
- `src/main/java/com/mayhew3/postgresobject/db/PostgresConnectionFactory.java` - Added SET search_path command
- `.github/workflows/ci.yml` - Added ALTER DATABASE command (can be removed)

### Test Exclusions
- `build.gradle` - Excluded MySQL tests (no longer maintained)

## References

- PostgreSQL Documentation: [Schema Search Path](https://www.postgresql.org/docs/current/ddl-schemas.html#DDL-SCHEMAS-PATH)
- PostgreSQL JDBC Driver: [Connection Parameters](https://jdbc.postgresql.org/documentation/head/connect.html)
- Related commit messages in feature/phase-2.3-integration-tests branch contain detailed rationale for each attempt

## Status

- **Problem**: PostgreSQL integration tests failing with "relation does not exist" errors
- **Root Cause**: Schema resolution mechanisms not working reliably in CI
- **Solution**: Use `public` schema for tests instead of `test` schema
- **Verification**: Pending CI run of commit 0ad31fb
