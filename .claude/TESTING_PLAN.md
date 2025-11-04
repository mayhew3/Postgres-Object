# Testing Plan: postgres-object

**Date:** November 3, 2025
**Version:** 0.19.8
**Status:** Comprehensive Test Strategy

---

## Executive Summary

This document outlines a comprehensive testing strategy for the `postgres-object` library. Currently, the library has 12 test classes with approximately 19 test files, covering basic functionality through unit and integration tests. However, there are significant gaps in error scenarios, concurrency, performance, and edge cases.

**Current Test Coverage:**
- ✅ Basic CRUD operations (INSERT, UPDATE)
- ✅ Schema validation against live databases
- ✅ Field type conversions
- ✅ Database connection creation
- ⚠️ Limited error scenario testing
- ❌ No concurrency testing
- ❌ No performance benchmarks
- ❌ Minimal MySQL-specific testing

**Goals:**
1. Achieve >85% code coverage
2. Cover all error scenarios
3. Validate both PostgreSQL and MySQL behavior
4. Add performance benchmarks
5. Test concurrent access patterns
6. Validate security constraints

---

## Test Categories

### 1. Unit Tests

**Purpose:** Test individual components in isolation with mocked dependencies

#### 1.1 DataObject Tests (Expand Existing)

**Location:** `src/test/java/com/mayhew3/postgresobject/dataobject/DataObjectTest.java`

**Current Coverage:**
- ✅ Initialize for insert
- ✅ Initialize from ResultSet
- ✅ Change tracking
- ✅ Simple INSERT/UPDATE with mocked connections

**Missing Tests:**

```java
@Test
public void testCommitWithoutInitialization() {
    // Should throw IllegalStateException
    DataObject obj = new DataObjectMock();
    assertThrows(IllegalStateException.class, () -> obj.commit(connection));
}

@Test
public void testChangeIdOnExistingObjectThrowsException() {
    // Line 168: Cannot change id field on existing object
    DataObject obj = new DataObjectMock();
    obj.initializeFromDBObject(mockResultSet());
    assertThrows(RuntimeException.class, () -> obj.id.changeValue(999));
}

@Test
public void testMultipleCommitsDoNotDuplicateData() {
    // Calling commit() twice on unchanged object should not create duplicate
}

@Test
public void testPreInsertHook() {
    // Validate preInsert() is called before INSERT
}

@Test
public void testDateAddedAutoPopulation() {
    // Validate date_added is set automatically on insert
}

@Test
public void testNullFieldHandling() {
    // Test explicit null vs unset field
}

@Test
public void testGetFieldValueWithNameReturnsCorrectField() {
    // Test field lookup by name
}

@Test
public void testGetFieldValueWithNameHandlesDuplicates() {
    // Should throw IllegalStateException for duplicate field names
}

@Test
public void testGenerateTableCreateStatementPostgres() {
    // Validate DDL generation for PostgreSQL
}

@Test
public void testGenerateTableCreateStatementMySQL() {
    // Validate DDL generation for MySQL
}

@Test
public void testUniqueConstraintGeneration() {
    // Validate UNIQUE constraints in DDL
}

@Test
public void testForeignKeyStatementGeneration() {
    // Validate ALTER TABLE statements for foreign keys
}
```

#### 1.2 FieldValue Tests

**Location:** `src/test/java/com/mayhew3/postgresobject/dataobject/`

**Current Coverage:**
- ✅ FieldValueBigDecimal basic functionality

**Missing Test Classes:**
- `FieldValueStringTest.java`
- `FieldValueIntegerTest.java`
- `FieldValueBooleanTest.java`
- `FieldValueTimestampTest.java`
- `FieldValueDateTest.java`
- `FieldValueForeignKeyTest.java`

**Test Template:**

```java
public class FieldValueStringTest {

    @Test
    public void testInitializeValue() {
        FieldValueString field = new FieldValueString("test", 100);
        field.initializeValue("hello");
        assertEquals("hello", field.getValue());
        assertEquals("hello", field.getOriginalValue());
        assertFalse(field.isChanged());
    }

    @Test
    public void testChangeValue() {
        FieldValueString field = new FieldValueString("test", 100);
        field.initializeValue("hello");
        field.changeValue("world");
        assertEquals("world", field.getValue());
        assertEquals("hello", field.getOriginalValue());
        assertTrue(field.isChanged());
    }

    @Test
    public void testChangeValueToNull() {
        FieldValueString field = new FieldValueString("test", 100);
        field.initializeValue("hello");
        field.changeValue(null);
        assertNull(field.getValue());
        assertTrue(field.getExplicitNull());
    }

    @Test
    public void testDiscardChange() {
        FieldValueString field = new FieldValueString("test", 100);
        field.initializeValue("hello");
        field.changeValue("world");
        field.discardChange();
        assertEquals("hello", field.getValue());
        assertFalse(field.isChanged());
    }

    @Test
    public void testChangeValueUnlessToNull() {
        FieldValueString field = new FieldValueString("test", 100);
        field.initializeValue("hello");
        field.changeValueUnlessToNull(null);
        assertEquals("hello", field.getValue()); // Should not change
        field.changeValueUnlessToNull("world");
        assertEquals("world", field.getValue()); // Should change
    }

    @Test
    public void testDDLTypePostgres() {
        FieldValueString field = new FieldValueString("test", 100);
        assertEquals("VARCHAR(100)", field.getDDLType(DatabaseType.POSTGRES));
    }

    @Test
    public void testDDLTypeMySQL() {
        FieldValueString field = new FieldValueString("test", 100);
        assertEquals("VARCHAR(100)", field.getDDLType(DatabaseType.MYSQL));
    }

    @Test
    public void testUpdatePreparedStatement() throws SQLException {
        // Test PreparedStatement parameter binding
    }

    @Test
    public void testInitializeFromResultSet() throws SQLException {
        // Test loading from ResultSet
    }
}
```

**Repeat for all FieldValue types with type-specific tests.**

#### 1.3 FieldConversion Tests

**New Location:** `src/test/java/com/mayhew3/postgresobject/dataobject/`

**Missing Test Classes:**
- `FieldConversionStringTest.java`
- `FieldConversionIntegerTest.java`
- `FieldConversionBigDecimalTest.java`
- `FieldConversionBooleanTest.java`
- `FieldConversionTimestampTest.java`
- `FieldConversionDateTest.java`

**Example:**

```java
public class FieldConversionIntegerTest {

    private FieldConversionInteger converter;

    @Before
    public void setUp() {
        converter = new FieldConversionInteger();
    }

    @Test
    public void testParseFromStringValid() {
        assertEquals(Integer.valueOf(42), converter.parseFromString("42"));
        assertEquals(Integer.valueOf(-100), converter.parseFromString("-100"));
        assertEquals(Integer.valueOf(0), converter.parseFromString("0"));
    }

    @Test(expected = NumberFormatException.class)
    public void testParseFromStringInvalid() {
        converter.parseFromString("not a number");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseFromStringDecimal() {
        converter.parseFromString("42.5");
    }

    @Test
    public void testParseFromStringWhitespace() {
        assertEquals(Integer.valueOf(42), converter.parseFromString("  42  "));
    }
}
```

#### 1.4 Connection Factory Tests

**Expand:** `src/test/java/com/mayhew3/postgresobject/db/PostgresConnectionTest.java`

**Missing Tests:**

```java
@Test
public void testCreateConnectionWithNullUrl() {
    DatabaseEnvironment env = mock(DatabaseEnvironment.class);
    when(env.getDatabaseUrl()).thenReturn(null);
    assertThrows(IllegalStateException.class,
        () -> PostgresConnectionFactory.createConnection(env));
}

@Test
public void testCreateConnectionWithInvalidUrl() {
    DatabaseEnvironment env = mock(DatabaseEnvironment.class);
    when(env.getDatabaseUrl()).thenReturn("invalid-url");
    assertThrows(URISyntaxException.class,
        () -> PostgresConnectionFactory.createConnection(env));
}

@Test
public void testCreateConnectionWithMissingCredentials() {
    // Test URL without username/password
}

@Test
public void testCreateConnectionWithSchemaName() {
    // Verify schema is set in connection string
}

@Test
public void testDriverRegistration() {
    // Verify PostgreSQL driver is registered
}
```

**New:** `src/test/java/com/mayhew3/postgresobject/db/MySQLConnectionFactoryTest.java`

```java
public class MySQLConnectionFactoryTest {

    @Test
    public void testCreateConnection() throws Exception {
        // Test MySQL connection creation
    }

    @Test
    public void testConnectionStringFormat() {
        // Verify MySQL-specific connection string format
    }
}
```

### 2. Integration Tests

**Purpose:** Test components working together with real databases

#### 2.1 Database Operation Tests (Expand Existing)

**Location:** `src/test/java/com/mayhew3/postgresobject/dataobject/DataObjectCommitTest.java`

**Current Coverage:**
- ✅ Insert DataObjectMock
- ✅ Update DataObjectMock

**Missing Tests:**

```java
@Test
public void testInsertAndUpdateWithForeignKey() {
    // Test foreign key relationships
}

@Test
public void testInsertMultipleObjectsInSequence() {
    // Test multiple inserts maintain correct IDs
}

@Test
public void testUpdateOnlyChangedFields() {
    // Verify only changed fields are in UPDATE statement
}

@Test
public void testUpdateWithNoChangesDoesNotHitDatabase() {
    // Verify no UPDATE if nothing changed
}

@Test
public void testInsertWithExplicitNull() {
    // Test explicit null vs unset field
}

@Test
public void testInsertWithDefaultValues() {
    // Test field default values
}

@Test
public void testConcurrentUpdatesToSameRow() {
    // Test last-write-wins behavior (or detect issue)
}

@Test
public void testTransactionRollback() {
    // Test manual transaction rollback
}

@Test
public void testLargeTextFieldInsert() {
    // Test TEXT fields with large content (1MB+)
}

@Test
public void testSpecialCharactersInFields() {
    // Test Unicode, quotes, SQL keywords
}

@Test
public void testDateTimeFieldAccuracy() {
    // Verify timestamp precision
}

@Test
public void testBigDecimalPrecision() {
    // Test decimal precision maintained
}
```

#### 2.2 Schema Validation Tests (Expand Existing)

**Location:** `src/test/java/com/mayhew3/postgresobject/model/`

**Current Coverage:**
- ✅ GenericDatabaseSchemaTest
- ✅ PostgresSchemaTest
- ✅ MySQLSchemaTest

**Missing Tests:**

```java
@Test
public void testValidationDetectsMissingTable() {
    // Drop table, verify mismatch detected
}

@Test
public void testValidationDetectsMissingColumn() {
    // Add column to code, verify mismatch detected
}

@Test
public void testValidationDetectsColumnTypeChange() {
    // Change column type, verify mismatch
}

@Test
public void testValidationDetectsMissingForeignKey() {
    // Add FK to code, verify mismatch detected
}

@Test
public void testValidationDetectsMissingIndex() {
    // Add index to code, verify mismatch
}

@Test
public void testValidationDetectsNullabilityMismatch() {
    // Change NOT NULL constraint
}

@Test
public void testValidationDetectsDefaultValueMismatch() {
    // Change default value
}

@Test
public void testValidationGeneratesCorrectDDL() {
    // Verify DDL statements in mismatch output
}

@Test
public void testValidationWithMultipleTables() {
    // Test schema with 10+ tables
}

@Test
public void testValidationPerformance() {
    // Benchmark validation speed on large schemas
}
```

#### 2.3 Database Recreator Tests

**Expand:** `src/test/java/com/mayhew3/postgresobject/dataobject/DatabaseRecreatorTest.java`

**Missing Tests:**

```java
@Test
public void testRecreateDropsExistingTables() {
    // Verify CASCADE drop behavior
}

@Test
public void testRecreateCreatesAllTables() {
    // Verify all tables in schema are created
}

@Test
public void testRecreateCreatesIndexes() {
    // Verify indexes are created
}

@Test
public void testRecreateCreatesForeignKeys() {
    // Verify foreign keys are created
}

@Test
public void testRecreateWithExistingData() {
    // Verify data is dropped
}

@Test
public void testRecreateHandlesCyclicForeignKeys() {
    // Test handling of circular FK dependencies
}
```

### 3. Error Scenario Tests

**Purpose:** Validate error handling and edge cases

#### 3.1 Connection Error Tests

**New Location:** `src/test/java/com/mayhew3/postgresobject/db/ConnectionErrorTest.java`

```java
public class ConnectionErrorTest {

    @Test
    public void testConnectionTimeout() {
        // Test connection timeout behavior
    }

    @Test
    public void testConnectionLost() throws SQLException {
        // Simulate connection loss mid-operation
        PostgresConnection conn = createConnection();
        // Close underlying connection
        conn.closeConnection();
        // Attempt operation - should reconnect
        assertDoesNotThrow(() -> conn.executeQuery("SELECT 1"));
    }

    @Test
    public void testConnectionExpiry() {
        // Test 30-minute expiry and reconnection
    }

    @Test
    public void testConnectionPoolExhaustion() {
        // Test behavior when no connections available
    }

    @Test
    public void testReconnectFailure() {
        // Test behavior when reconnect fails
    }

    @Test
    public void testAuthenticationFailure() {
        // Test invalid credentials
    }

    @Test
    public void testDatabaseNotFound() {
        // Test connecting to non-existent database
    }

    @Test
    public void testNetworkPartition() {
        // Simulate network failure
    }
}
```

#### 3.2 SQL Error Tests

**New Location:** `src/test/java/com/mayhew3/postgresobject/db/SQLErrorTest.java`

```java
public class SQLErrorTest extends DatabaseTest {

    @Test
    public void testUniqueConstraintViolation() {
        // Insert duplicate unique value
        // Verify appropriate exception
    }

    @Test
    public void testForeignKeyViolation() {
        // Insert with invalid FK
    }

    @Test
    public void testNotNullConstraintViolation() {
        // Insert null into NOT NULL column
    }

    @Test
    public void testInvalidSQLSyntax() {
        // Test malformed SQL
    }

    @Test
    public void testTableDoesNotExist() {
        // Query non-existent table
    }

    @Test
    public void testColumnDoesNotExist() {
        // Query non-existent column
    }

    @Test
    public void testDataTypeMismatch() {
        // Insert string into integer column
    }

    @Test
    public void testStringTooLong() {
        // Insert string longer than VARCHAR limit
    }

    @Test
    public void testNumericOverflow() {
        // Insert number too large for column
    }
}
```

#### 3.3 Data Validation Tests

**New Location:** `src/test/java/com/mayhew3/postgresobject/dataobject/DataValidationTest.java`

```java
public class DataValidationTest {

    @Test
    public void testNullInRequiredField() {
        // Attempt to insert null in NOT NULL field
    }

    @Test
    public void testInvalidDataType() {
        // Attempt to set wrong type on field
    }

    @Test
    public void testStringFieldMaxLength() {
        // Test string at boundary of max length
    }

    @Test
    public void testIntegerBoundaries() {
        // Test Integer.MIN_VALUE and MAX_VALUE
    }

    @Test
    public void testTimestampBoundaries() {
        // Test edge cases for timestamps
    }

    @Test
    public void testBigDecimalScale() {
        // Test decimal scale is preserved
    }
}
```

### 4. Concurrency Tests

**Purpose:** Validate thread safety and concurrent access patterns

**New Location:** `src/test/java/com/mayhew3/postgresobject/concurrency/`

#### 4.1 Concurrent Reads

```java
public class ConcurrentReadTest extends DatabaseTest {

    @Test
    public void testMultipleThreadsReadingSameRow() throws InterruptedException {
        // 10 threads reading same row simultaneously
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Setup: Insert test data
        DataObjectMock obj = new DataObjectMock();
        obj.initializeForInsert();
        obj.title.changeValue("Test");
        obj.commit(connection);
        Integer id = obj.id.getValue();

        // Execute: Multiple concurrent reads
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ResultSet rs = connection.prepareAndExecuteStatementFetch(
                        "SELECT * FROM test WHERE id = ?", id);
                    assertTrue(rs.next());
                    assertEquals("Test", rs.getString("title"));
                    latch.countDown();
                } catch (SQLException e) {
                    fail("Concurrent read failed: " + e.getMessage());
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
    }

    @Test
    public void testMultipleConnectionsSimultaneously() {
        // Test connection pool behavior
    }
}
```

#### 4.2 Concurrent Writes

```java
public class ConcurrentWriteTest extends DatabaseTest {

    @Test
    public void testConcurrentInsertsToSameTable() throws InterruptedException {
        // 10 threads inserting to same table
        // Verify all inserts succeed with unique IDs
    }

    @Test
    public void testConcurrentUpdatesToSameRow() throws InterruptedException {
        // Multiple threads updating same row
        // Document last-write-wins behavior
    }

    @Test
    public void testConcurrentUpdatesToDifferentRows() throws InterruptedException {
        // Should not conflict
    }

    @Test
    public void testDeadlockDetection() {
        // Create scenario that could cause deadlock
        // Verify database handles it appropriately
    }
}
```

#### 4.3 Thread Safety Tests

```java
public class ThreadSafetyTest {

    @Test
    public void testDataObjectNotThreadSafe() {
        // Document that DataObject is not thread-safe
        // Multiple threads modifying same DataObject should fail
    }

    @Test
    public void testConnectionThreadSafety() {
        // Test if SQLConnection can be used by multiple threads
    }

    @Test
    public void testFieldValueThreadSafety() {
        // Test if FieldValue is thread-safe (it's not)
    }
}
```

### 5. Performance Tests

**Purpose:** Establish performance baselines and identify bottlenecks

**New Location:** `src/test/java/com/mayhew3/postgresobject/performance/`

#### 5.1 Benchmark Tests

```java
public class BenchmarkTest extends DatabaseTest {

    @Test
    public void benchmarkSingleInsert() {
        // Measure time for single insert
        long startTime = System.nanoTime();

        DataObjectMock obj = new DataObjectMock();
        obj.initializeForInsert();
        obj.title.changeValue("Benchmark");
        obj.commit(connection);

        long duration = System.nanoTime() - startTime;
        System.out.println("Single insert: " + duration / 1_000_000 + "ms");

        // Assert reasonable performance
        assertTrue(duration < 100_000_000, "Insert took longer than 100ms");
    }

    @Test
    public void benchmarkBulkInsert() {
        // Insert 1000 rows, measure time
        int rowCount = 1000;
        long startTime = System.nanoTime();

        for (int i = 0; i < rowCount; i++) {
            DataObjectMock obj = new DataObjectMock();
            obj.initializeForInsert();
            obj.title.changeValue("Row " + i);
            obj.commit(connection);
        }

        long duration = System.nanoTime() - startTime;
        double avgTime = (duration / 1_000_000.0) / rowCount;
        System.out.println("Bulk insert (" + rowCount + " rows): " + avgTime + "ms per row");
    }

    @Test
    public void benchmarkSingleUpdate() {
        // Measure update performance
    }

    @Test
    public void benchmarkBulkUpdate() {
        // Update 1000 rows
    }

    @Test
    public void benchmarkSimpleQuery() {
        // SELECT by primary key
    }

    @Test
    public void benchmarkComplexQuery() {
        // SELECT with JOINs
    }

    @Test
    public void benchmarkSchemaValidation() {
        // Measure validation time for schema with 50 tables
    }
}
```

#### 5.2 Load Tests

```java
public class LoadTest extends DatabaseTest {

    @Test
    public void loadTestConcurrentInserts() {
        // 100 concurrent threads inserting
    }

    @Test
    public void loadTestConcurrentUpdates() {
        // 100 concurrent threads updating
    }

    @Test
    public void loadTestMixedWorkload() {
        // Reads, writes, updates simultaneously
    }

    @Test
    public void loadTestConnectionPoolExhaustion() {
        // Attempt to exhaust connection pool
    }
}
```

### 6. Database-Specific Tests

**Purpose:** Validate vendor-specific behavior

#### 6.1 PostgreSQL-Specific Tests

**New Location:** `src/test/java/com/mayhew3/postgresobject/db/PostgresSpecificTest.java`

```java
public class PostgresSpecificTest extends DatabaseTest {

    @Test
    public void testSerialIdGeneration() {
        // Verify SERIAL type behavior
    }

    @Test
    public void testReturningClause() {
        // Verify RETURNING clause in INSERT
    }

    @Test
    public void testDollarQuoting() {
        // Test strings with $$ quoting
    }

    @Test
    public void testArrayTypes() {
        // Test if array types are supported (they're not)
    }

    @Test
    public void testJSONTypes() {
        // Test if JSON/JSONB are supported (they're not)
    }

    @Test
    public void testUUIDType() {
        // Test UUID support
    }

    @Test
    public void testEnumTypes() {
        // Test enum support
    }

    @Test
    public void testTimestampWithTimeZone() {
        // Test timezone handling
    }
}
```

#### 6.2 MySQL-Specific Tests

**New Location:** `src/test/java/com/mayhew3/postgresobject/db/MySQLSpecificTest.java`

```java
public class MySQLSpecificTest extends DatabaseTest {

    @Test
    public void testAutoIncrementIdGeneration() {
        // Verify AUTO_INCREMENT behavior
    }

    @Test
    public void testBacktickIdentifierQuoting() {
        // Verify backticks are used for identifiers
    }

    @Test
    public void testMySQLDataTypes() {
        // Test MySQL-specific types (TINYINT, MEDIUMINT, etc.)
    }

    @Test
    public void testMySQLEnumType() {
        // Test MySQL ENUM support
    }

    @Test
    public void testCaseSensitivity() {
        // Test case sensitivity differences
    }
}
```

### 7. Integration Tests (External Systems)

**Purpose:** Test interaction with external systems

#### 7.1 Archive/Restore Tests

**New Location:** `src/test/java/com/mayhew3/postgresobject/db/DataArchiverTest.java`

```java
public class DataArchiverTest extends DatabaseTest {

    @Test
    public void testArchiveOldData() {
        // Archive data older than 6 months
        // Verify CSV created correctly
    }

    @Test
    public void testArchiveVerifiesSchema() {
        // Verify header validation works
    }

    @Test
    public void testArchiveHandlesSpecialCharacters() {
        // Test CSV escaping
    }

    @Test
    public void testArchiveDeletesAfterBackup() {
        // Verify data is deleted from database
    }

    @Test
    public void testRestoreFromArchive() {
        // Create restore test
    }

    @Test
    public void testArchiveFailureDoesNotDeleteData() {
        // Verify atomicity
    }
}
```

### 8. Security Tests

**Purpose:** Validate security constraints

**New Location:** `src/test/java/com/mayhew3/postgresobject/security/`

```java
public class SecurityTest extends DatabaseTest {

    @Test
    public void testSQLInjectionPrevention() {
        // Attempt SQL injection via field values
        DataObjectMock obj = new DataObjectMock();
        obj.initializeForInsert();
        obj.title.changeValue("'; DROP TABLE test; --");
        obj.commit(connection);

        // Verify table still exists
        ResultSet rs = connection.executeQuery("SELECT COUNT(*) FROM test");
        assertTrue(rs.next());
    }

    @Test
    public void testTableNameInjection() {
        // Verify table names are validated
    }

    @Test
    public void testColumnNameInjection() {
        // Verify column names are validated
    }

    @Test
    public void testConnectionStringDoesNotLogPasswords() {
        // Verify credentials not in logs
    }

    @Test
    public void testPreparedStatementParameterBinding() {
        // Verify all user input goes through PreparedStatement
    }
}
```

---

## Test Infrastructure

### Test Utilities

**New Location:** `src/test/java/com/mayhew3/postgresobject/util/TestUtils.java`

```java
public class TestUtils {

    public static DataObjectMock createMockObject(String title, Integer kernels) {
        DataObjectMock obj = new DataObjectMock();
        obj.initializeForInsert();
        obj.title.changeValue(title);
        obj.kernels.changeValue(kernels);
        return obj;
    }

    public static void insertTestData(SQLConnection conn, int count) throws SQLException {
        for (int i = 0; i < count; i++) {
            DataObjectMock obj = createMockObject("Test " + i, i);
            obj.commit(conn);
        }
    }

    public static void assertDatabaseEmpty(SQLConnection conn, String tableName) throws SQLException {
        ResultSet rs = conn.executeQuery("SELECT COUNT(*) as cnt FROM " + tableName);
        rs.next();
        assertEquals(0, rs.getInt("cnt"));
    }

    public static void waitForCondition(Callable<Boolean> condition, long timeoutMs)
            throws Exception {
        long startTime = System.currentTimeMillis();
        while (!condition.call()) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new TimeoutException("Condition not met within " + timeoutMs + "ms");
            }
            Thread.sleep(100);
        }
    }
}
```

### Test Data Builders

```java
public class DataObjectBuilder {

    private String title = "Default Title";
    private Integer kernels = 0;

    public DataObjectBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public DataObjectBuilder withKernels(Integer kernels) {
        this.kernels = kernels;
        return this;
    }

    public DataObjectMock build() {
        DataObjectMock obj = new DataObjectMock();
        obj.initializeForInsert();
        obj.title.changeValue(title);
        obj.kernels.changeValue(kernels);
        return obj;
    }

    public DataObjectMock buildAndCommit(SQLConnection conn) throws SQLException {
        DataObjectMock obj = build();
        obj.commit(conn);
        return obj;
    }
}
```

### Custom Assertions

```java
public class DataObjectAssertions {

    public static void assertInInsertMode(DataObject obj) {
        assertTrue(obj.isInitialized());
        assertTrue(obj.isForInsert());
        assertFalse(obj.isForUpdate());
    }

    public static void assertInUpdateMode(DataObject obj) {
        assertTrue(obj.isInitialized());
        assertFalse(obj.isForInsert());
        assertTrue(obj.isForUpdate());
    }

    public static void assertFieldChanged(FieldValue<?> field) {
        assertTrue(field.isChanged());
        assertNotEquals(field.getOriginalValue(), field.getChangedValue());
    }

    public static void assertFieldUnchanged(FieldValue<?> field) {
        assertFalse(field.isChanged());
        assertEquals(field.getOriginalValue(), field.getChangedValue());
    }
}
```

---

## Test Execution Strategy

### 1. Test Phases

**Phase 1: Unit Tests** (Fast - Run on every build)
- All unit tests with mocked dependencies
- ~200ms execution time target
- No database required

**Phase 2: Integration Tests** (Medium - Run on commit)
- Tests requiring database
- ~10-30s execution time
- Uses local test database

**Phase 3: Load/Performance Tests** (Slow - Run nightly)
- Performance benchmarks
- Concurrency tests
- ~5-15 minutes execution time

### 2. Test Databases

**Local Test Database:**
```bash
# PostgreSQL
createdb postgres_object_test
# Used by: Integration tests, schema tests

# MySQL
mysql -e "CREATE DATABASE postgres_object_test"
# Used by: MySQL-specific tests
```

**CI/CD Test Database:**
- Use Docker containers for isolated test databases
- Separate container per test class for parallelization

### 3. Gradle Configuration

**Update `build.gradle`:**

```gradle
test {
    // Separate unit and integration tests
    useJUnit {
        includeCategories 'com.mayhew3.postgresobject.test.UnitTest'
    }
}

task integrationTest(type: Test) {
    useJUnit {
        includeCategories 'com.mayhew3.postgresobject.test.IntegrationTest'
    }
    shouldRunAfter test
}

task performanceTest(type: Test) {
    useJUnit {
        includeCategories 'com.mayhew3.postgresobject.test.PerformanceTest'
    }
    shouldRunAfter integrationTest
}

// Code coverage
jacoco {
    toolVersion = "0.8.8"
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

check.dependsOn jacocoTestCoverageVerification
```

### 4. Test Categories

**Create marker interfaces:**

```java
public interface UnitTest {}
public interface IntegrationTest {}
public interface PerformanceTest {}
public interface SecurityTest {}
```

**Tag tests:**

```java
@Category(UnitTest.class)
public class DataObjectTest { ... }

@Category(IntegrationTest.class)
public class DataObjectCommitTest extends DatabaseTest { ... }

@Category(PerformanceTest.class)
public class BenchmarkTest extends DatabaseTest { ... }
```

---

## Code Coverage Goals

### Coverage Targets

| Component | Target | Current (Estimated) |
|-----------|--------|---------------------|
| DataObject | 90% | ~60% |
| FieldValue classes | 85% | ~40% |
| SQLConnection implementations | 80% | ~50% |
| DataSchema | 85% | ~70% |
| Connection factories | 75% | ~40% |
| Utilities (Archiver, etc.) | 70% | ~30% |
| **Overall** | **85%** | **~45%** |

### Exclusions

Exclude from coverage:
- Getter/setter methods (if trivial)
- toString() methods
- Main classes for executables
- Deprecated methods

---

## Testing Checklist

### For Each New Feature

- [ ] Unit tests for new classes/methods
- [ ] Integration tests with real database
- [ ] Error scenario tests
- [ ] Documentation updated
- [ ] Performance impact assessed
- [ ] Security review completed
- [ ] Both PostgreSQL and MySQL tested

### For Each Bug Fix

- [ ] Regression test added
- [ ] Root cause documented
- [ ] Related code paths tested
- [ ] Integration test validates fix

### For Each Release

- [ ] All tests passing
- [ ] Code coverage meets targets
- [ ] Performance benchmarks run
- [ ] Security tests passing
- [ ] Manual smoke tests completed

---

## Priority Recommendations

### HIGH PRIORITY (Implement First)

1. **Error Scenario Tests** - Critical for production reliability
   - Connection failures
   - SQL errors
   - Data validation failures

2. **Security Tests** - Prevent vulnerabilities
   - SQL injection prevention
   - Credential handling
   - Input validation

3. **Field Value Tests** - Core functionality
   - Complete test coverage for all field types
   - Boundary conditions
   - Type conversions

### MEDIUM PRIORITY (Implement Second)

4. **Concurrency Tests** - Important for multi-threaded apps
   - Thread safety validation
   - Concurrent access patterns
   - Deadlock scenarios

5. **Database-Specific Tests** - Ensure vendor compatibility
   - PostgreSQL edge cases
   - MySQL edge cases
   - Cross-database validation

6. **Performance Tests** - Establish baselines
   - Benchmark common operations
   - Identify bottlenecks
   - Track performance regressions

### LOW PRIORITY (Implement Last)

7. **Load Tests** - Nice to have
   - High-concurrency scenarios
   - Stress testing
   - Connection pool validation

---

## Continuous Integration

### GitHub Actions / Jenkins Configuration

```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Run unit tests
        run: ./gradlew test
      - name: Upload coverage
        uses: codecov/codecov-action@v2

  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:13
        env:
          POSTGRES_PASSWORD: test
          POSTGRES_DB: postgres_object_test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Run integration tests
        run: ./gradlew integrationTest
        env:
          DB_URL: postgresql://localhost:5432/postgres_object_test
          DB_USER: postgres
          DB_PASSWORD: test

  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run security tests
        run: ./gradlew test --tests '*SecurityTest'
```

---

## Maintenance

### Regular Review Schedule

**Monthly:**
- Review test execution times
- Check for flaky tests
- Update deprecated test dependencies

**Quarterly:**
- Review code coverage trends
- Update performance baselines
- Assess new testing tools/frameworks

**Annually:**
- Comprehensive test strategy review
- Update testing documentation
- Evaluate migration to newer test frameworks (JUnit 5, etc.)

---

## Conclusion

This testing plan provides a roadmap to achieve comprehensive test coverage for the `postgres-object` library. Implementing these tests will:

1. Increase confidence in production reliability
2. Enable safe refactoring and feature additions
3. Document expected behavior
4. Catch regressions early
5. Validate cross-database compatibility

**Estimated Implementation Timeline:**
- High Priority Tests: 2-3 weeks
- Medium Priority Tests: 2-3 weeks
- Low Priority Tests: 1-2 weeks
- **Total: 5-8 weeks** of focused testing effort

**Return on Investment:**
- Significantly reduced production bugs
- Faster feature development with confidence
- Easier onboarding for new developers
- Better documentation through tests
- Foundation for future enhancements

Start with high-priority tests to get maximum impact quickly, then progressively add coverage in medium and low priority areas.
