package com.mayhew3.postgresobject.dataobject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for uncovered DataObject methods.
 */
public class DataObjectMethodsTest {

  private TestDataObject testObject;

  private static class TestDataObject extends DataObject {
    FieldValueString nameField = registerStringField("name", Nullability.NOT_NULL);
    FieldValueInteger ageField = registerIntegerField("age", Nullability.NULLABLE);
    FieldValueBoolean activeField = registerBooleanField("active", Nullability.NOT_NULL);

    @Override
    public String getTableName() {
      return "test_table";
    }
  }

  @BeforeEach
  public void setUp() {
    testObject = new TestDataObject();
  }

  // Tests for hasChanged()
  @Test
  public void testHasChangedReturnsFalseForNewObject() {
    testObject.initializeForInsert();
    assertFalse(testObject.hasChanged(), "New insert object should not have changes");
  }

  @Test
  public void testHasChangedReturnsFalseAfterFieldChangeInInsertMode() {
    testObject.initializeForInsert();
    testObject.nameField.changeValue("John");

    // hasChanged() returns !isForInsert() && !getChangedFields().isEmpty()
    // So in insert mode, it returns false even with changed fields
    assertFalse(testObject.hasChanged(), "Insert mode object should return false for hasChanged()");
  }

  @Test
  public void testHasChangedReturnsFalseForInsertMode() {
    testObject.initializeForInsert();
    testObject.nameField.changeValue("John");

    // In insert mode, hasChanged() returns false even with changed fields
    assertFalse(testObject.hasChanged(), "Insert mode object should return false for hasChanged()");
  }

  @Test
  public void testHasChangedReturnsTrueForUpdateModeWithChanges() {
    testObject.initializeForInsert();
    testObject.changeToUpdateObject();
    testObject.nameField.changeValue("John");

    assertTrue(testObject.hasChanged(), "Update mode object with changes should return true");
  }

  @Test
  public void testHasChangedReturnsFalseForUpdateModeWithoutChanges() {
    testObject.initializeForInsert();
    testObject.changeToUpdateObject();

    assertFalse(testObject.hasChanged(), "Update mode object without changes should return false");
  }

  // Tests for getChangedFields()
  @Test
  public void testGetChangedFieldsReturnsEmptyListForNewObject() {
    testObject.initializeForInsert();
    List<FieldValue> changedFields = testObject.getChangedFields();

    assertNotNull(changedFields, "Changed fields list should not be null");
    assertEquals(0, changedFields.size(), "New object should have no changed fields");
  }

  @Test
  public void testGetChangedFieldsReturnsModifiedField() {
    testObject.initializeForInsert();
    testObject.nameField.changeValue("John");

    List<FieldValue> changedFields = testObject.getChangedFields();

    assertEquals(1, changedFields.size(), "Should have one changed field");
    assertTrue(changedFields.contains(testObject.nameField), "Changed fields should contain nameField");
  }

  @Test
  public void testGetChangedFieldsReturnsMultipleModifiedFields() {
    testObject.initializeForInsert();
    testObject.nameField.changeValue("John");
    testObject.ageField.changeValue(30);
    testObject.activeField.changeValue(true);

    List<FieldValue> changedFields = testObject.getChangedFields();

    assertEquals(3, changedFields.size(), "Should have three changed fields");
    assertTrue(changedFields.contains(testObject.nameField), "Changed fields should contain nameField");
    assertTrue(changedFields.contains(testObject.ageField), "Changed fields should contain ageField");
    assertTrue(changedFields.contains(testObject.activeField), "Changed fields should contain activeField");
  }

  // Tests for getAllFieldValues()
  @Test
  public void testGetAllFieldValuesExcludesId() {
    testObject.initializeForInsert();
    List<FieldValue> allFields = testObject.getAllFieldValues();

    assertNotNull(allFields, "Field list should not be null");
    // DataObject automatically adds dateAdded field, so we have 4 fields: name, age, active, dateAdded
    assertEquals(4, allFields.size(), "Should have 4 fields (excluding id)");
    assertFalse(allFields.contains(testObject.id), "Should not contain id field");
  }

  @Test
  public void testGetAllFieldValuesContainsRegisteredFields() {
    testObject.initializeForInsert();
    List<FieldValue> allFields = testObject.getAllFieldValues();

    assertTrue(allFields.contains(testObject.nameField), "Should contain nameField");
    assertTrue(allFields.contains(testObject.ageField), "Should contain ageField");
    assertTrue(allFields.contains(testObject.activeField), "Should contain activeField");
  }

  // Tests for getAllFieldValuesIncludingId()
  @Test
  public void testGetAllFieldValuesIncludingIdContainsId() {
    testObject.initializeForInsert();
    List<FieldValue> allFieldsWithId = testObject.getAllFieldValuesIncludingId();

    assertNotNull(allFieldsWithId, "Field list should not be null");
    // 4 fields (name, age, active, dateAdded) + id = 5 fields
    assertEquals(5, allFieldsWithId.size(), "Should have 5 fields (including id)");
    assertTrue(allFieldsWithId.contains(testObject.id), "Should contain id field");
  }

  @Test
  public void testGetAllFieldValuesIncludingIdContainsAllFields() {
    testObject.initializeForInsert();
    List<FieldValue> allFieldsWithId = testObject.getAllFieldValuesIncludingId();

    assertTrue(allFieldsWithId.contains(testObject.id), "Should contain id field");
    assertTrue(allFieldsWithId.contains(testObject.nameField), "Should contain nameField");
    assertTrue(allFieldsWithId.contains(testObject.ageField), "Should contain ageField");
    assertTrue(allFieldsWithId.contains(testObject.activeField), "Should contain activeField");
  }

  // Tests for getForeignKeys()
  @Test
  public void testGetForeignKeysReturnsEmptyListWhenNoForeignKeys() {
    List<FieldValueForeignKey> foreignKeys = testObject.getForeignKeys();

    assertNotNull(foreignKeys, "Foreign keys list should not be null");
    assertEquals(0, foreignKeys.size(), "Should have no foreign keys");
  }

  // Tests for getIndices()
  @Test
  public void testGetIndicesReturnsEmptyListWhenNoIndices() {
    List<ColumnsIndex> indices = testObject.getIndices();

    assertNotNull(indices, "Indices list should not be null");
    assertEquals(0, indices.size(), "Should have no indices");
  }

  @Test
  public void testGetIndicesReturnsAddedIndex() {
    testObject.addColumnsIndex(testObject.nameField);
    List<ColumnsIndex> indices = testObject.getIndices();

    assertEquals(1, indices.size(), "Should have one index");
  }

  // Tests for getUniqueIndices()
  @Test
  public void testGetUniqueIndicesIncludesAutomaticIdConstraint() {
    List<UniqueConstraint> uniqueIndices = testObject.getUniqueIndices();

    assertNotNull(uniqueIndices, "Unique indices list should not be null");
    // DataObject constructor automatically adds a unique constraint on id
    assertEquals(1, uniqueIndices.size(), "Should have one unique index (id)");
  }

  @Test
  public void testGetUniqueIndicesReturnsAddedUniqueConstraint() {
    testObject.addUniqueConstraint(testObject.nameField);
    List<UniqueConstraint> uniqueIndices = testObject.getUniqueIndices();

    // 1 automatic (id) + 1 added (nameField) = 2
    assertEquals(2, uniqueIndices.size(), "Should have two unique constraints");
  }

  // Tests for getSequenceNames()
  @Test
  public void testGetSequenceNamesReturnsTableNameWithSuffix() {
    List<String> sequenceNames = testObject.getSequenceNames();

    assertNotNull(sequenceNames, "Sequence names list should not be null");
    assertEquals(1, sequenceNames.size(), "Should have one sequence name");
    // Sequence name format is: tablename_id_seq
    assertEquals("test_table_id_seq", sequenceNames.get(0), "Sequence name should follow pattern");
  }

  // Tests for field registration methods that aren't covered
  @Test
  public void testRegisterSerialFieldCreatesSerialField() {
    testObject.initializeForInsert();

    // The id field is already registered as a serial field in the constructor
    assertNotNull(testObject.id, "ID field should be registered");
    assertTrue(testObject.id instanceof FieldValueSerial, "ID field should be FieldValueSerial");
  }

  // Test for various field types being properly registered
  @Test
  public void testAllRegisteredFieldTypesArePresentInFieldList() {
    testObject.initializeForInsert();
    List<FieldValue> allFields = testObject.getAllFieldValues();

    // Verify that all types we registered are present
    boolean hasStringField = allFields.stream().anyMatch(f -> f instanceof FieldValueString);
    boolean hasIntegerField = allFields.stream().anyMatch(f -> f instanceof FieldValueInteger);
    boolean hasBooleanField = allFields.stream().anyMatch(f -> f instanceof FieldValueBoolean);

    assertTrue(hasStringField, "Should have a String field");
    assertTrue(hasIntegerField, "Should have an Integer field");
    assertTrue(hasBooleanField, "Should have a Boolean field");
  }

  // Test createDDLStatement throws UnsupportedOperationException (since it's not implemented)
  @Test
  public void testCreateDDLStatementThrowsUnsupportedOperation() {
    // createDDLStatement() is designed to be overridden by subtypes
    // The base implementation throws UnsupportedOperationException
    assertThrows(UnsupportedOperationException.class, () -> testObject.createDDLStatement());
  }
}
