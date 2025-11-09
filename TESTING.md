# Testing Guide

This document explains how to run tests for the postgres-object library, including unit tests and integration tests.

## Quick Start

### Unit Tests Only (No Database Required)

```bash
./gradlew test
```

Unit tests run without database connections and are always enabled. They test business logic, data validation, and behavior in isolation using mocks.

### All Tests (Including Integration Tests)

Integration tests require database connections. There are several ways to run them:

#### Recommended: Using the Local Test Script

The easiest way to run all tests locally is using the provided script:

```bash
# Windows
test-local.bat

# Linux/Mac
./test-local.sh
```

This script automatically sets `POSTGRES_SCHEMA=postgres_object_test` and runs the full test suite against your local PostgreSQL database.

You can also pass additional Gradle arguments:

```bash
# Run specific test class
./test-local.sh --tests PostgreSQLCRUDIntegrationTest

# Run with verbose output
./test-local.sh --info
```

#### Alternative Methods:

#### Option 1: Using Docker Compose (Recommended)

```bash
# Start test databases
docker-compose -f docker-compose.test.yml up -d

# Wait for databases to be ready (healthchecks run automatically)
sleep 10

# Set environment variable and run all tests
export postgres_local_password=testpassword
./gradlew test

# Stop databases when done
docker-compose -f docker-compose.test.yml down
```

#### Option 2: Using Local Database Installation

If you have PostgreSQL and MySQL installed locally:

```bash
# Set required environment variable
export postgres_local_password=your_postgres_password

# Run all tests
./gradlew test
```

## Test Organization

### Unit Tests (Always Run)
- `DataObjectTest` - DataObject behavior with mocks
- `DataObjectMethodsTest` - DataObject utility methods
- `FieldValueBehaviorTest` - Core FieldValue behavior
- `FieldValueBooleanTest` - Boolean field type testing
- `FieldValueDateTest` - Date field type testing
- `FieldValueTimestampTest` - Timestamp field type testing
- `FieldValueBigDecimalTest` - BigDecimal field type testing

**Coverage:** 120+ unit tests

### Integration Tests (Require Databases)
- `PostgreSQLCRUDIntegrationTest` - PostgreSQL CRUD operations
- `DataObjectCommitTest` - End-to-end commit operations
- `DatabaseRecreatorTest` - Schema recreation
- `PostgresConnectionTest` - Connection management
- `GenericDatabaseSchemaTest` - Schema validation
- `DataObjectTableValidatorTest` - Table validation

**Coverage:** 30+ integration tests

## CI/CD

### GitHub Actions

Integration tests run automatically in CI using GitHub Actions services:

- **PostgreSQL 17** on port 5440
- **MySQL 8.0** on port 3306

The workflow:
1. Spins up database containers
2. Initializes test schemas
3. Sets environment variables
4. Runs all tests (unit + integration)
5. Generates coverage reports

See `.github/workflows/ci.yml` for details.

### Local CI Simulation

To run tests exactly as CI does:

```bash
# Set CI environment variable to enable integration tests
export CI=true
export postgres_local_password=testpassword

# Start databases
docker-compose -f docker-compose.test.yml up -d

# Run tests
./gradlew clean test jacocoTestReport

# Clean up
docker-compose -f docker-compose.test.yml down
```

## Database Configuration

### PostgreSQL Test Database

- **Image:** postgres:17
- **Port:** 5440 (mapped from container port 5432)
- **Database:** projects
- **Schema:** test
- **User:** postgres
- **Password:** testpassword (in Docker/CI)

### MySQL Test Database

- **Image:** mysql:8.0
- **Port:** 3306
- **Database:** projects
- **User:** testuser / root
- **Password:** testpassword (in Docker/CI)

## Environment Variables

Required for integration tests:

- `postgres_local_password` - PostgreSQL password (set to "testpassword" for Docker/CI)

Optional (automatically set in CI):
- `CI=true` - Enables integration tests in build.gradle
- `POSTGRES_HOST` - Default: localhost
- `POSTGRES_PORT` - Default: 5440 (configurable via environment variable)
- `POSTGRES_VERSION` - Default: 17 (configurable via environment variable)
- `POSTGRES_SCHEMA` - Default: test (configurable via environment variable)
- `MYSQL_HOST` - Default: localhost
- `MYSQL_PORT` - Default: 3306

## Coverage Reports

After running tests, coverage reports are generated:

- **HTML Report:** `build/reports/jacoco/test/html/index.html`
- **XML Report:** `build/reports/jacoco/test/jacocoTestReport.xml`

Open the HTML report in a browser to see detailed coverage metrics.

## Troubleshooting

### Integration Tests Don't Run Locally

**Problem:** Integration tests are skipped when running `./gradlew test`

**Solution:** Integration tests are automatically excluded when running locally without CI environment. Either:
1. Use Docker Compose (see above)
2. Set `export CI=true` to enable them
3. Configure local PostgreSQL/MySQL instances

### Connection Refused Errors

**Problem:** Tests fail with connection refused

**Solution:**
1. Verify databases are running: `docker-compose -f docker-compose.test.yml ps`
2. Check ports are not already in use: `lsof -i :5440` and `lsof -i :3306`
3. Wait for healthchecks to pass before running tests

### Schema Not Found Errors

**Problem:** Tests fail with "schema does not exist"

**Solution:** The initialization script creates the "test" schema automatically. If using local databases, create it manually:

```sql
CREATE SCHEMA IF NOT EXISTS test;
GRANT ALL PRIVILEGES ON SCHEMA test TO postgres;
```

### Wrong Port for PostgreSQL

**Problem:** Tests try to connect to wrong port

**Solution:** The library uses port 5440 by default (for PostgreSQL 17), but this is configurable:
- Set `POSTGRES_PORT` environment variable to use a different port
- The default can be changed in `InternalDatabaseEnvironments.java`
- Ensure Docker/CI maps to the same port your configuration expects

## Development Workflow

When adding new tests:

1. **Unit tests:** Always add unit tests first (no database required)
2. **Integration tests:** Add integration tests for database-specific behavior
3. **Run locally:** Verify with `./gradlew test`
4. **Run with Docker:** Test integration with `docker-compose up`
5. **Push:** CI will run all tests automatically

## Best Practices

- ✅ Write unit tests for all business logic
- ✅ Use integration tests for database interactions
- ✅ Keep integration tests fast (< 1 second per test)
- ✅ Clean up test data (DatabaseTest does this automatically)
- ✅ Test both PostgreSQL and MySQL when applicable
- ❌ Don't mock database connections in integration tests
- ❌ Don't commit sensitive credentials (use environment variables)
