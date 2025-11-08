package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.DatabaseType;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for FieldValueTimestamp behavior.
 */
public class FieldValueTimestampTest {

  private TestDataObject testObject;
  private Timestamp testTimestamp1;
  private Timestamp testTimestamp2;
  private Date testDate1;
  private Date testDate2;

  private static class TestDataObject extends DataObject {
    FieldValueTimestamp timestampField = registerTimestampField("timestamp_field", Nullability.NOT_NULL);
    FieldValueTimestamp nullableTimestampField = registerTimestampField("nullable_timestamp_field", Nullability.NULLABLE);
    FieldValueTimestamp timestampWithDefaultNow = registerTimestampField("timestamp_with_default_now", Nullability.NULLABLE).defaultValueNow();

    @Override
    public String getTableName() {
      return "test_table";
    }
  }

  @Before
  public void setUp() {
    testObject = new TestDataObject();
    testObject.initializeForInsert();

    // Create test timestamps
    Calendar cal1 = Calendar.getInstance();
    cal1.set(2023, Calendar.JANUARY, 15, 10, 30, 45);
    cal1.set(Calendar.MILLISECOND, 123);
    testTimestamp1 = new Timestamp(cal1.getTimeInMillis());
    testDate1 = new Date(cal1.getTimeInMillis());

    Calendar cal2 = Calendar.getInstance();
    cal2.set(2024, Calendar.DECEMBER, 31, 23, 59, 59);
    cal2.set(Calendar.MILLISECOND, 999);
    testTimestamp2 = new Timestamp(cal2.getTimeInMillis());
    testDate2 = new Date(cal2.getTimeInMillis());
  }

  @Test
  public void testTimestampFieldAcceptsTimestamp() {
    testObject.timestampField.changeValue(testTimestamp1);
    assertEquals(testTimestamp1, testObject.timestampField.getValue());
    assertTrue(testObject.timestampField.isChanged());
  }

  @Test
  public void testTimestampFieldAcceptsDate() {
    // Test the overloaded changeValue(Date) method
    testObject.timestampField.changeValue(testDate1);
    assertNotNull(testObject.timestampField.getValue());
    assertEquals(testDate1.getTime(), testObject.timestampField.getValue().getTime());
    assertTrue(testObject.timestampField.isChanged());
  }

  @Test
  public void testNullableTimestampFieldAcceptsNull() {
    // First set to a non-null value to establish a baseline
    testObject.nullableTimestampField.changeValue(testTimestamp1);
    testObject.nullableTimestampField.updateInternal(); // Make testTimestamp1 the "original" value

    // Now change to null
    testObject.nullableTimestampField.changeValue((Timestamp) null);
    assertNull(testObject.nullableTimestampField.getValue());
    assertTrue(testObject.nullableTimestampField.isChanged());
  }

  @Test
  public void testTimestampFieldChangesValue() {
    testObject.timestampField.changeValue(testTimestamp1);
    assertEquals(testTimestamp1, testObject.timestampField.getValue());

    testObject.timestampField.changeValue(testTimestamp2);
    assertEquals(testTimestamp2, testObject.timestampField.getValue());
    assertTrue(testObject.timestampField.isChanged());
  }

  @Test
  public void testTimestampFieldTracksOriginalValue() {
    testObject.timestampField.initializeValue(testTimestamp1);
    assertEquals(testTimestamp1, testObject.timestampField.getOriginalValue());

    testObject.timestampField.changeValue(testTimestamp2);
    assertEquals(testTimestamp1, testObject.timestampField.getOriginalValue());
    assertEquals(testTimestamp2, testObject.timestampField.getChangedValue());
  }

  @Test
  public void testTimestampFieldDDLTypeForPostgres() {
    String ddlType = testObject.timestampField.getDDLType(DatabaseType.POSTGRES);
    assertEquals("TIMESTAMP(6) WITH TIME ZONE", ddlType);
  }

  @Test
  public void testTimestampFieldDDLTypeForMySQL() {
    String ddlType = testObject.timestampField.getDDLType(DatabaseType.MYSQL);
    assertEquals("TIMESTAMP", ddlType);
  }

  @Test
  public void testTimestampFieldInformationSchemaTypeForPostgres() {
    String schemaType = testObject.timestampField.getInformationSchemaType(DatabaseType.POSTGRES);
    assertEquals("timestamp with time zone", schemaType);
  }

  @Test
  public void testTimestampFieldInformationSchemaTypeForMySQL() {
    String schemaType = testObject.timestampField.getInformationSchemaType(DatabaseType.MYSQL);
    assertEquals("TIMESTAMP", schemaType);
  }

  @Test
  public void testTimestampFieldIsChanged() {
    assertFalse(testObject.timestampField.isChanged());

    testObject.timestampField.changeValue(testTimestamp1);
    assertTrue(testObject.timestampField.isChanged());
  }

  @Test
  public void testChangeValueUnlessToNullBehaviorWithTimestamp() {
    testObject.timestampField.changeValue(testTimestamp1);
    testObject.timestampField.changeValueUnlessToNull((Timestamp) null);
    // Should NOT change to null (because changeValueUnlessToNull rejects null)
    assertEquals(testTimestamp1, testObject.timestampField.getValue());

    testObject.timestampField.changeValueUnlessToNull(testTimestamp2);
    // Should change to testTimestamp2
    assertEquals(testTimestamp2, testObject.timestampField.getValue());
  }

  @Test
  public void testChangeValueUnlessToNullBehaviorWithDate() {
    // Test the overloaded changeValueUnlessToNull(Date) method
    testObject.timestampField.changeValue(testTimestamp1);
    testObject.timestampField.changeValueUnlessToNull((Date) null);
    // Should NOT change to null
    assertEquals(testTimestamp1, testObject.timestampField.getValue());

    testObject.timestampField.changeValueUnlessToNull(testDate2);
    // Should change to testDate2 (converted to Timestamp)
    assertEquals(testDate2.getTime(), testObject.timestampField.getValue().getTime());
  }

  @Test
  public void testInitializeValueDoesNotMarkAsChanged() {
    testObject.timestampField.initializeValue(testTimestamp1);
    assertFalse(testObject.timestampField.isChanged());
    assertEquals(testTimestamp1, testObject.timestampField.getValue());
  }

  @Test
  public void testNullValueHelperMethod() {
    testObject.nullableTimestampField.changeValue(testTimestamp1);
    assertEquals(testTimestamp1, testObject.nullableTimestampField.getValue());
    testObject.nullableTimestampField.updateInternal(); // Make testTimestamp1 the "original" value

    testObject.nullableTimestampField.nullValue();
    assertNull(testObject.nullableTimestampField.getValue());
    // nullValue() sets changedValue to null, making it different from originalValue
    assertTrue(testObject.nullableTimestampField.isChanged());
  }

  @Test
  public void testFieldNameIsCorrect() {
    assertEquals("timestamp_field", testObject.timestampField.getFieldName());
    assertEquals("nullable_timestamp_field", testObject.nullableTimestampField.getFieldName());
  }

  @Test
  public void testDefaultValueNowForPostgres() {
    String defaultValue = testObject.timestampWithDefaultNow.getDefaultValue(DatabaseType.POSTGRES);
    assertEquals("now()", defaultValue);
  }

  @Test
  public void testDefaultValueNowForMySQL() {
    String defaultValue = testObject.timestampWithDefaultNow.getDefaultValue(DatabaseType.MYSQL);
    assertEquals("CURRENT_TIMESTAMP", defaultValue);
  }

  @Test
  public void testDefaultValueNowReturnsSelfForChaining() {
    FieldValueTimestamp field = testObject.timestampField.defaultValueNow();
    assertNotNull(field);
    assertEquals(testObject.timestampField, field);
  }

  @Test
  public void testTimestampWithoutDefaultReturnsNull() {
    String defaultValue = testObject.timestampField.getDefaultValue(DatabaseType.POSTGRES);
    assertNull(defaultValue);

    defaultValue = testObject.timestampField.getDefaultValue(DatabaseType.MYSQL);
    assertNull(defaultValue);
  }

  @Test
  public void testChangeValueFromXMLString() {
    // Test the special XML string conversion method
    long secondsSinceEpoch = 1609459200L; // 2021-01-01 00:00:00 UTC
    testObject.timestampField.changeValueFromXMLString(String.valueOf(secondsSinceEpoch));

    Timestamp expectedTimestamp = new Timestamp(secondsSinceEpoch * 1000);
    assertEquals(expectedTimestamp, testObject.timestampField.getValue());
    assertTrue(testObject.timestampField.isChanged());
  }

  @Test
  public void testChangeValueFromXMLStringWithNull() {
    testObject.timestampField.changeValue(testTimestamp1);
    testObject.timestampField.changeValueFromXMLString(null);

    // Should not change when null is passed
    assertEquals(testTimestamp1, testObject.timestampField.getValue());
  }

  @Test
  public void testMultipleTimestampChangesKeepLatest() {
    testObject.timestampField.changeValue(testTimestamp1);
    testObject.timestampField.changeValue(testTimestamp2);

    assertEquals(testTimestamp2, testObject.timestampField.getValue());
    assertTrue(testObject.timestampField.isChanged());
  }

  @Test
  public void testTimestampFieldAcceptsPastTimestamp() {
    Calendar cal = Calendar.getInstance();
    cal.set(1900, Calendar.JANUARY, 1, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Timestamp pastTimestamp = new Timestamp(cal.getTimeInMillis());

    testObject.timestampField.changeValue(pastTimestamp);
    assertEquals(pastTimestamp, testObject.timestampField.getValue());
  }

  @Test
  public void testTimestampFieldAcceptsFutureTimestamp() {
    Calendar cal = Calendar.getInstance();
    cal.set(2100, Calendar.DECEMBER, 31, 23, 59, 59);
    cal.set(Calendar.MILLISECOND, 999);
    Timestamp futureTimestamp = new Timestamp(cal.getTimeInMillis());

    testObject.timestampField.changeValue(futureTimestamp);
    assertEquals(futureTimestamp, testObject.timestampField.getValue());
  }

  @Test
  public void testTimestampPreservesMilliseconds() {
    // Verify that milliseconds are preserved through the conversion
    Calendar cal = Calendar.getInstance();
    cal.set(2023, Calendar.JUNE, 15, 14, 30, 45);
    cal.set(Calendar.MILLISECOND, 567);
    Timestamp timestampWithMillis = new Timestamp(cal.getTimeInMillis());

    testObject.timestampField.changeValue(timestampWithMillis);
    assertEquals(567, testObject.timestampField.getValue().getNanos() / 1000000);
  }

  @Test
  public void testChangeValueWithDateConvertsToTimestamp() {
    testObject.timestampField.changeValue(testDate1);

    // Verify it's stored as a Timestamp
    assertNotNull(testObject.timestampField.getValue());
    assertTrue(testObject.timestampField.getValue() instanceof Timestamp);
    assertEquals(testDate1.getTime(), testObject.timestampField.getValue().getTime());
  }

  @Test
  public void testChangeValueWithNullDateSetsNull() {
    // Test the overloaded changeValue(Date) with null
    testObject.timestampField.changeValue(testTimestamp1);
    testObject.timestampField.updateInternal();

    testObject.timestampField.changeValue((Date) null);
    assertNull(testObject.timestampField.getValue());
    assertTrue(testObject.timestampField.isChanged());
  }

  @Test
  public void testTimestampFieldHandlesCurrentTime() {
    // Test with current timestamp
    Timestamp now = new Timestamp(System.currentTimeMillis());
    testObject.timestampField.changeValue(now);

    assertEquals(now, testObject.timestampField.getValue());
    assertTrue(testObject.timestampField.isChanged());
  }
}
