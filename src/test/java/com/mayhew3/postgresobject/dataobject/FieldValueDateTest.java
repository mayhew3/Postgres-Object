package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.DatabaseType;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for FieldValueDate behavior.
 */
public class FieldValueDateTest {

  private TestDataObject testObject;
  private Date testDate1;
  private Date testDate2;

  private static class TestDataObject extends DataObject {
    FieldValueDate dateField = registerDateField("date_field", Nullability.NOT_NULL);
    FieldValueDate nullableDateField = registerDateField("nullable_date_field", Nullability.NULLABLE);
    FieldValueDate dateWithDefault = registerDateField("date_with_default", Nullability.NULLABLE);

    @Override
    public String getTableName() {
      return "test_table";
    }
  }

  @Before
  public void setUp() {
    testObject = new TestDataObject();
    testObject.initializeForInsert();

    // Create test dates
    Calendar cal1 = Calendar.getInstance();
    cal1.set(2023, Calendar.JANUARY, 15, 0, 0, 0);
    cal1.set(Calendar.MILLISECOND, 0);
    testDate1 = cal1.getTime();

    Calendar cal2 = Calendar.getInstance();
    cal2.set(2024, Calendar.DECEMBER, 31, 0, 0, 0);
    cal2.set(Calendar.MILLISECOND, 0);
    testDate2 = cal2.getTime();
  }

  @Test
  public void testDateFieldAcceptsDate() {
    testObject.dateField.changeValue(testDate1);
    assertEquals(testDate1, testObject.dateField.getValue());
    assertTrue(testObject.dateField.isChanged());
  }

  @Test
  public void testNullableDateFieldAcceptsNull() {
    // First set to a non-null value to establish a baseline
    testObject.nullableDateField.changeValue(testDate1);
    testObject.nullableDateField.updateInternal(); // Make testDate1 the "original" value

    // Now change to null
    testObject.nullableDateField.changeValue(null);
    assertNull(testObject.nullableDateField.getValue());
    assertTrue(testObject.nullableDateField.isChanged());
  }

  @Test
  public void testDateFieldChangesValue() {
    testObject.dateField.changeValue(testDate1);
    assertEquals(testDate1, testObject.dateField.getValue());

    testObject.dateField.changeValue(testDate2);
    assertEquals(testDate2, testObject.dateField.getValue());
    assertTrue(testObject.dateField.isChanged());
  }

  @Test
  public void testDateFieldTracksOriginalValue() {
    testObject.dateField.initializeValue(testDate1);
    assertEquals(testDate1, testObject.dateField.getOriginalValue());

    testObject.dateField.changeValue(testDate2);
    assertEquals(testDate1, testObject.dateField.getOriginalValue());
    assertEquals(testDate2, testObject.dateField.getChangedValue());
  }

  @Test
  public void testDateFieldDDLTypeForPostgres() {
    String ddlType = testObject.dateField.getDDLType(DatabaseType.POSTGRES);
    assertEquals("DATE", ddlType);
  }

  @Test
  public void testDateFieldDDLTypeForMySQL() {
    String ddlType = testObject.dateField.getDDLType(DatabaseType.MYSQL);
    assertEquals("DATE", ddlType);
  }

  @Test
  public void testDateFieldInformationSchemaTypeForPostgres() {
    String schemaType = testObject.dateField.getInformationSchemaType(DatabaseType.POSTGRES);
    assertEquals("date", schemaType);
  }

  @Test
  public void testDateFieldInformationSchemaTypeForMySQL() {
    String schemaType = testObject.dateField.getInformationSchemaType(DatabaseType.MYSQL);
    assertEquals("date", schemaType);
  }

  @Test
  public void testDateFieldIsChanged() {
    assertFalse(testObject.dateField.isChanged());

    testObject.dateField.changeValue(testDate1);
    assertTrue(testObject.dateField.isChanged());
  }

  @Test
  public void testChangeValueUnlessToNullBehaviorWithDate() {
    testObject.dateField.changeValue(testDate1);
    testObject.dateField.changeValueUnlessToNull(null);
    // Should NOT change to null (because changeValueUnlessToNull rejects null)
    assertEquals(testDate1, testObject.dateField.getValue());

    testObject.dateField.changeValueUnlessToNull(testDate2);
    // Should change to testDate2
    assertEquals(testDate2, testObject.dateField.getValue());
  }

  @Test
  public void testInitializeValueDoesNotMarkAsChanged() {
    testObject.dateField.initializeValue(testDate1);
    assertFalse(testObject.dateField.isChanged());
    assertEquals(testDate1, testObject.dateField.getValue());
  }

  @Test
  public void testNullValueHelperMethod() {
    testObject.nullableDateField.changeValue(testDate1);
    assertEquals(testDate1, testObject.nullableDateField.getValue());
    testObject.nullableDateField.updateInternal(); // Make testDate1 the "original" value

    testObject.nullableDateField.nullValue();
    assertNull(testObject.nullableDateField.getValue());
    // nullValue() sets changedValue to null, making it different from originalValue (testDate1)
    assertTrue(testObject.nullableDateField.isChanged());
  }

  @Test
  public void testFieldNameIsCorrect() {
    assertEquals("date_field", testObject.dateField.getFieldName());
    assertEquals("nullable_date_field", testObject.nullableDateField.getFieldName());
  }

  @Test
  public void testDateFieldWithDefaultValue() {
    Date defaultDate = new Date();
    testObject.dateWithDefault.defaultValue(defaultDate);

    // defaultValue() only affects DDL generation, not initialization
    assertNull(testObject.dateWithDefault.getValue());
  }

  @Test
  public void testChangeValueFromXMLString() {
    // Test the special XML string conversion method
    long secondsSinceEpoch = 1609459200L; // 2021-01-01 00:00:00 UTC
    testObject.dateField.changeValueFromXMLString(String.valueOf(secondsSinceEpoch));

    Date expectedDate = new Date(secondsSinceEpoch * 1000);
    assertEquals(expectedDate, testObject.dateField.getValue());
    assertTrue(testObject.dateField.isChanged());
  }

  @Test
  public void testChangeValueFromXMLStringWithNull() {
    testObject.dateField.changeValue(testDate1);
    testObject.dateField.changeValueFromXMLString(null);

    // Should not change when null is passed
    assertEquals(testDate1, testObject.dateField.getValue());
  }

  @Test
  public void testMultipleDateChangesKeepLatest() {
    testObject.dateField.changeValue(testDate1);
    testObject.dateField.changeValue(testDate2);

    assertEquals(testDate2, testObject.dateField.getValue());
    assertTrue(testObject.dateField.isChanged());
  }

  @Test
  public void testDateFieldAcceptsPastDate() {
    Calendar cal = Calendar.getInstance();
    cal.set(1900, Calendar.JANUARY, 1, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Date pastDate = cal.getTime();

    testObject.dateField.changeValue(pastDate);
    assertEquals(pastDate, testObject.dateField.getValue());
  }

  @Test
  public void testDateFieldAcceptsFutureDate() {
    Calendar cal = Calendar.getInstance();
    cal.set(2100, Calendar.DECEMBER, 31, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Date futureDate = cal.getTime();

    testObject.dateField.changeValue(futureDate);
    assertEquals(futureDate, testObject.dateField.getValue());
  }

  @Test
  public void testDefaultValueMethod() {
    Date defaultDate = testDate1;
    FieldValueDate field = testObject.dateWithDefault.defaultValue(defaultDate);

    // Should return the field itself for method chaining
    assertNotNull(field);
    assertEquals(testObject.dateWithDefault, field);
  }
}
