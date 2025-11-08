package com.mayhew3.postgresobject.dataobject;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

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

  @Before
  public void setUp() {
    testObject = new TestDataObject();
  }

  // Tests for hasChanged()
  @Test
  public void testHasChangedReturnsFalseForNewObject() {
    testObject.initializeForInsert();
    assertFalse("New insert object should not have changes", testObject.hasChanged());
  }

  @Test
  public void testHasChangedReturnsFalseAfterFieldChangeInInsertMode() {
    testObject.initializeForInsert();
    testObject.nameField.changeValue("John");

    // hasChanged() returns !isForInsert() && !getChangedFields().isEmpty()
    // So in insert mode, it returns false even with changed fields
    assertFalse("Insert mode object should return false for hasChanged()", testObject.hasChanged());
  }

  @Test
  public void testHasChangedReturnsFalseForInsertMode() {
    testObject.initializeForInsert();
    testObject.nameField.changeValue("John");

    // In insert mode, hasChanged() returns false even with changed fields
    assertFalse("Insert mode object should return false for hasChanged()", testObject.hasChanged());
  }

  @Test
  public void testHasChangedReturnsTrueForUpdateModeWithChanges() {
    testObject.initializeForInsert();
    testObject.changeToUpdateObject();
    testObject.nameField.changeValue("John");

    assertTrue("Update mode object with changes should return true", testObject.hasChanged());
  }

  @Test
  public void testHasChangedReturnsFalseForUpdateModeWithoutChanges() {
    testObject.initializeForInsert();
    testObject.changeToUpdateObject();

    assertFalse("Update mode object without changes should return false", testObject.hasChanged());
  }

  // Tests for getChangedFields()
  @Test
  public void testGetChangedFieldsReturnsEmptyListForNewObject() {
    testObject.initializeForInsert();
    List<FieldValue> changedFields = testObject.getChangedFields();

    assertNotNull("Changed fields list should not be null", changedFields);
    assertEquals("New object should have no changed fields", 0, changedFields.size());
  }

  @Test
  public void testGetChangedFieldsReturnsModifiedField() {
    testObject.initializeForInsert();
    testObject.nameField.changeValue("John");

    List<FieldValue> changedFields = testObject.getChangedFields();

    assertEquals("Should have one changed field", 1, changedFields.size());
    assertTrue("Changed fields should contain nameField", changedFields.contains(testObject.nameField));
  }

  @Test
  public void testGetChangedFieldsReturnsMultipleModifiedFields() {
    testObject.initializeForInsert();
    testObject.nameField.changeValue("John");
    testObject.ageField.changeValue(30);
    testObject.activeField.changeValue(true);

    List<FieldValue> changedFields = testObject.getChangedFields();

    assertEquals("Should have three changed fields", 3, changedFields.size());
    assertTrue("Changed fields should contain nameField", changedFields.contains(testObject.nameField));
    assertTrue("Changed fields should contain ageField", changedFields.contains(testObject.ageField));
    assertTrue("Changed fields should contain activeField", changedFields.contains(testObject.activeField));
  }

  // Tests for getAllFieldValues()
  @Test
  public void testGetAllFieldValuesExcludesId() {
    testObject.initializeForInsert();
    List<FieldValue> allFields = testObject.getAllFieldValues();

    assertNotNull("Field list should not be null", allFields);
    // DataObject automatically adds dateAdded field, so we have 4 fields: name, age, active, dateAdded
    assertEquals("Should have 4 fields (excluding id)", 4, allFields.size());
    assertFalse("Should not contain id field", allFields.contains(testObject.id));
  }

  @Test
  public void testGetAllFieldValuesContainsRegisteredFields() {
    testObject.initializeForInsert();
    List<FieldValue> allFields = testObject.getAllFieldValues();

    assertTrue("Should contain nameField", allFields.contains(testObject.nameField));
    assertTrue("Should contain ageField", allFields.contains(testObject.ageField));
    assertTrue("Should contain activeField", allFields.contains(testObject.activeField));
  }

  // Tests for getAllFieldValuesIncludingId()
  @Test
  public void testGetAllFieldValuesIncludingIdContainsId() {
    testObject.initializeForInsert();
    List<FieldValue> allFieldsWithId = testObject.getAllFieldValuesIncludingId();

    assertNotNull("Field list should not be null", allFieldsWithId);
    // 4 fields (name, age, active, dateAdded) + id = 5 fields
    assertEquals("Should have 5 fields (including id)", 5, allFieldsWithId.size());
    assertTrue("Should contain id field", allFieldsWithId.contains(testObject.id));
  }

  @Test
  public void testGetAllFieldValuesIncludingIdContainsAllFields() {
    testObject.initializeForInsert();
    List<FieldValue> allFieldsWithId = testObject.getAllFieldValuesIncludingId();

    assertTrue("Should contain id field", allFieldsWithId.contains(testObject.id));
    assertTrue("Should contain nameField", allFieldsWithId.contains(testObject.nameField));
    assertTrue("Should contain ageField", allFieldsWithId.contains(testObject.ageField));
    assertTrue("Should contain activeField", allFieldsWithId.contains(testObject.activeField));
  }

  // Tests for getForeignKeys()
  @Test
  public void testGetForeignKeysReturnsEmptyListWhenNoForeignKeys() {
    List<FieldValueForeignKey> foreignKeys = testObject.getForeignKeys();

    assertNotNull("Foreign keys list should not be null", foreignKeys);
    assertEquals("Should have no foreign keys", 0, foreignKeys.size());
  }

  // Tests for getIndices()
  @Test
  public void testGetIndicesReturnsEmptyListWhenNoIndices() {
    List<ColumnsIndex> indices = testObject.getIndices();

    assertNotNull("Indices list should not be null", indices);
    assertEquals("Should have no indices", 0, indices.size());
  }

  @Test
  public void testGetIndicesReturnsAddedIndex() {
    testObject.addColumnsIndex(testObject.nameField);
    List<ColumnsIndex> indices = testObject.getIndices();

    assertEquals("Should have one index", 1, indices.size());
  }

  // Tests for getUniqueIndices()
  @Test
  public void testGetUniqueIndicesIncludesAutomaticIdConstraint() {
    List<UniqueConstraint> uniqueIndices = testObject.getUniqueIndices();

    assertNotNull("Unique indices list should not be null", uniqueIndices);
    // DataObject constructor automatically adds a unique constraint on id
    assertEquals("Should have one unique index (id)", 1, uniqueIndices.size());
  }

  @Test
  public void testGetUniqueIndicesReturnsAddedUniqueConstraint() {
    testObject.addUniqueConstraint(testObject.nameField);
    List<UniqueConstraint> uniqueIndices = testObject.getUniqueIndices();

    // 1 automatic (id) + 1 added (nameField) = 2
    assertEquals("Should have two unique constraints", 2, uniqueIndices.size());
  }

  // Tests for getSequenceNames()
  @Test
  public void testGetSequenceNamesReturnsTableNameWithSuffix() {
    List<String> sequenceNames = testObject.getSequenceNames();

    assertNotNull("Sequence names list should not be null", sequenceNames);
    assertEquals("Should have one sequence name", 1, sequenceNames.size());
    // Sequence name format is: tablename_id_seq
    assertEquals("Sequence name should follow pattern", "test_table_id_seq", sequenceNames.get(0));
  }

  // Tests for field registration methods that aren't covered
  @Test
  public void testRegisterSerialFieldCreatesSerialField() {
    testObject.initializeForInsert();

    // The id field is already registered as a serial field in the constructor
    assertNotNull("ID field should be registered", testObject.id);
    assertTrue("ID field should be FieldValueSerial", testObject.id instanceof FieldValueSerial);
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

    assertTrue("Should have a String field", hasStringField);
    assertTrue("Should have an Integer field", hasIntegerField);
    assertTrue("Should have a Boolean field", hasBooleanField);
  }

  // Test createDDLStatement throws UnsupportedOperationException (since it's not implemented)
  @Test(expected = UnsupportedOperationException.class)
  public void testCreateDDLStatementThrowsUnsupportedOperation() {
    // createDDLStatement() is designed to be overridden by subtypes
    // The base implementation throws UnsupportedOperationException
    testObject.createDDLStatement();
  }
}
