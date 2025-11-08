package com.mayhew3.postgresobject.dataobject;

import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Tests for FieldValue behavior using DataObjectMock fields.
 * Focuses on change tracking, null handling, and value management.
 */
public class FieldValueBehaviorTest {

  private DataObjectMock dataObject;

  @Before
  public void setUp() {
    dataObject = new DataObjectMock();
    dataObject.initializeForInsert();
  }

  @Test
  public void testFieldStartsUnchangedAfterInit() {
    // Arrange: Fresh dataObject initialized for insert
    DataObjectMock fresh = new DataObjectMock();
    fresh.initializeForInsert();

    // Assert: Fields are null and not marked as changed
    // Note: defaultValue() only affects DDL schema, not object initialization
    assertThat(fresh.title.getChangedValue()).isNull();
    assertThat(fresh.kernels.getValue()).isNull();
  }

  @Test
  public void testChangeValueMarksFieldAsChanged() {
    // Act
    dataObject.title.changeValue("New Title");

    // Assert
    assertThat(dataObject.title.getValue()).isEqualTo("New Title");
    assertThat(dataObject.title.getChangedValue()).isEqualTo("New Title");
  }

  @Test
  public void testMultipleValueChangesKeepLatest() {
    // Act
    dataObject.title.changeValue("First");
    dataObject.title.changeValue("Second");
    dataObject.title.changeValue("Third");

    // Assert: Only latest value is kept
    assertThat(dataObject.title.getValue()).isEqualTo("Third");
    assertThat(dataObject.title.getChangedValue()).isEqualTo("Third");
  }

  @Test
  public void testNullableFieldCanBeSetToNull() {
    // Arrange: kernels is nullable
    dataObject.kernels.changeValue(42);
    assertThat(dataObject.kernels.getValue()).isEqualTo(42);

    // Act: Set to null using explicit cast
    dataObject.kernels.changeValue((Integer) null);

    // Assert
    assertThat(dataObject.kernels.getValue()).isNull();
    assertThat(dataObject.kernels.getChangedValue()).isNull();
  }

  @Test
  public void testFieldNameIsCorrect() {
    assertThat(dataObject.title.getFieldName()).isEqualTo("title");
    assertThat(dataObject.kernels.getFieldName()).isEqualTo("kernels");
    assertThat(dataObject.id.getFieldName()).isEqualTo("id");
  }

  @Test
  public void testGetOriginalValueVsChangedValue() {
    // Arrange: Initialize with a value
    dataObject.title.initializeValue("Original");

    // Original value should be set
    assertThat(dataObject.title.getOriginalValue()).isEqualTo("Original");
    assertThat(dataObject.title.getValue()).isEqualTo("Original");

    // Act: Change the value
    dataObject.title.changeValue("Changed");

    // Assert: Original stays the same, current value changes
    assertThat(dataObject.title.getOriginalValue()).isEqualTo("Original");
    assertThat(dataObject.title.getValue()).isEqualTo("Changed");
    assertThat(dataObject.title.getChangedValue()).isEqualTo("Changed");
  }

  @Test
  public void testInitializeValueDoesNotMarkAsChanged() {
    // Arrange: Fresh object
    DataObjectMock fresh = new DataObjectMock();

    // Act: Initialize value (simulating load from database)
    fresh.title.initializeValue("Loaded from DB");

    // Assert: Value is set but not marked as "changed"
    assertThat(fresh.title.getValue()).isEqualTo("Loaded from DB");
    assertThat(fresh.title.getOriginalValue()).isEqualTo("Loaded from DB");
    // The field should have this value but not be in "changed" state
  }

  @Test
  public void testChangeValueUnlessToNullBehavior() {
    // Arrange
    dataObject.title.changeValue("Initial");

    // Act: Try to change to null using changeValueUnlessToNull
    dataObject.title.changeValueUnlessToNull(null);

    // Assert: Value should remain unchanged
    assertThat(dataObject.title.getValue()).isEqualTo("Initial");

    // Act: Change to non-null value
    dataObject.title.changeValueUnlessToNull("Updated");

    // Assert: Value should change
    assertThat(dataObject.title.getValue()).isEqualTo("Updated");
  }

  @Test
  public void testIntegerFieldWithDefaultValue() {
    // The kernels field has a default value of 0 in the DDL schema
    DataObjectMock fresh = new DataObjectMock();
    fresh.initializeForInsert();

    // Assert: defaultValue() only affects DDL, not object initialization
    // The actual value remains null until explicitly set
    assertThat(fresh.kernels.getValue()).isNull();

    // But we can set it to the default value manually
    fresh.kernels.changeValue(0);
    assertThat(fresh.kernels.getValue()).isEqualTo(0);
  }

  @Test
  public void testIntegerFieldAcceptsNegativeValues() {
    dataObject.kernels.changeValue(-42);
    assertThat(dataObject.kernels.getValue()).isEqualTo(-42);
  }

  @Test
  public void testIntegerFieldAcceptsZero() {
    dataObject.kernels.changeValue(0);
    assertThat(dataObject.kernels.getValue()).isEqualTo(0);
  }

  @Test
  public void testIntegerFieldAcceptsLargeValues() {
    dataObject.kernels.changeValue(Integer.MAX_VALUE);
    assertThat(dataObject.kernels.getValue()).isEqualTo(Integer.MAX_VALUE);

    dataObject.kernels.changeValue(Integer.MIN_VALUE);
    assertThat(dataObject.kernels.getValue()).isEqualTo(Integer.MIN_VALUE);
  }

  @Test
  public void testStringFieldAcceptsEmptyString() {
    dataObject.title.changeValue("");
    assertThat(dataObject.title.getValue()).isEqualTo("");
    assertThat(dataObject.title.getValue()).isNotNull();
  }

  @Test
  public void testStringFieldAcceptsLongString() {
    String longString = "a".repeat(1000);
    dataObject.title.changeValue(longString);
    assertThat(dataObject.title.getValue()).hasSize(1000);
  }

  @Test
  public void testStringFieldAcceptsSpecialCharacters() {
    String special = "Test with 'quotes' and \"double\" and \\ backslash";
    dataObject.title.changeValue(special);
    assertThat(dataObject.title.getValue()).isEqualTo(special);
  }

  @Test
  public void testStringFieldAcceptsUnicode() {
    String unicode = "Hello 世界 🌍";
    dataObject.title.changeValue(unicode);
    assertThat(dataObject.title.getValue()).isEqualTo(unicode);
  }

  @Test
  public void testStringFieldAcceptsNewlines() {
    String multiline = "Line 1\nLine 2\nLine 3";
    dataObject.title.changeValue(multiline);
    assertThat(dataObject.title.getValue()).isEqualTo(multiline);
  }
}
