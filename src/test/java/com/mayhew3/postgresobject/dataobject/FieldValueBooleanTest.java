package com.mayhew3.postgresobject.dataobject;

import com.mayhew3.postgresobject.db.DatabaseType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for FieldValueBoolean behavior.
 */
public class FieldValueBooleanTest {

  private TestDataObject testObject;

  private static class TestDataObject extends DataObject {
    FieldValueBoolean boolField = registerBooleanField("bool_field", Nullability.NOT_NULL);
    FieldValueBoolean nullableBoolField = registerBooleanField("nullable_bool_field", Nullability.NULLABLE);
    FieldValueBoolean boolWithDefault = registerBooleanField("bool_with_default", Nullability.NOT_NULL).defaultValue(true);

    @Override
    public String getTableName() {
      return "test_table";
    }
  }

  @Before
  public void setUp() {
    testObject = new TestDataObject();
    testObject.initializeForInsert();
  }

  @Test
  public void testBooleanFieldAcceptsTrue() {
    testObject.boolField.changeValue(true);
    assertTrue(testObject.boolField.getValue());
    assertTrue(testObject.boolField.isChanged());
  }

  @Test
  public void testBooleanFieldAcceptsFalse() {
    testObject.boolField.changeValue(false);
    assertFalse(testObject.boolField.getValue());
    assertTrue(testObject.boolField.isChanged());
  }

  @Test
  public void testNullableBooleanFieldAcceptsNull() {
    // First set to a non-null value to establish a baseline
    testObject.nullableBoolField.changeValue(true);
    testObject.nullableBoolField.updateInternal(); // Make true the "original" value

    // Now change to null
    testObject.nullableBoolField.changeValue(null);
    assertNull(testObject.nullableBoolField.getValue());
    assertTrue(testObject.nullableBoolField.isChanged());
  }

  @Test
  public void testNonNullableBooleanFieldConvertsNullToFalse() {
    // When initializeValue is called with null on a NOT_NULL field, it should default to false
    testObject.boolField.initializeValue((Boolean) null);
    assertFalse(testObject.boolField.getValue());
    assertFalse(testObject.boolField.isChanged()); // initializeValue should not mark as changed
  }

  @Test
  public void testBooleanFieldChangesValue() {
    testObject.boolField.changeValue(true);
    assertEquals(true, testObject.boolField.getValue());

    testObject.boolField.changeValue(false);
    assertEquals(false, testObject.boolField.getValue());
    assertTrue(testObject.boolField.isChanged());
  }

  @Test
  public void testBooleanFieldTracksOriginalValue() {
    testObject.boolField.initializeValue(true);
    assertEquals(true, testObject.boolField.getOriginalValue());

    testObject.boolField.changeValue(false);
    assertEquals(true, testObject.boolField.getOriginalValue());
    assertEquals(false, testObject.boolField.getChangedValue());
  }

  @Test
  public void testBooleanFieldDefaultValueForPostgres() {
    String defaultValue = testObject.boolWithDefault.getDefaultValue(DatabaseType.POSTGRES);
    assertEquals("true", defaultValue);
  }

  @Test
  public void testBooleanFieldDefaultValueForMySQL() {
    testObject.boolWithDefault.defaultValue(true);
    String defaultValue = testObject.boolWithDefault.getDefaultValue(DatabaseType.MYSQL);
    assertEquals("1", defaultValue);

    testObject.boolWithDefault.defaultValue(false);
    defaultValue = testObject.boolWithDefault.getDefaultValue(DatabaseType.MYSQL);
    assertEquals("0", defaultValue);
  }

  @Test
  public void testBooleanFieldDDLTypeForPostgres() {
    String ddlType = testObject.boolField.getDDLType(DatabaseType.POSTGRES);
    assertEquals("BOOLEAN", ddlType);
  }

  @Test
  public void testBooleanFieldDDLTypeForMySQL() {
    String ddlType = testObject.boolField.getDDLType(DatabaseType.MYSQL);
    assertEquals("tinyint", ddlType);
  }

  @Test
  public void testBooleanFieldInformationSchemaTypeForPostgres() {
    String schemaType = testObject.boolField.getInformationSchemaType(DatabaseType.POSTGRES);
    assertEquals("BOOLEAN", schemaType);
  }

  @Test
  public void testBooleanFieldInformationSchemaTypeForMySQL() {
    String schemaType = testObject.boolField.getInformationSchemaType(DatabaseType.MYSQL);
    assertEquals("tinyint", schemaType);
  }

  @Test
  public void testSupportedDatabaseTypes() {
    // Test that both supported database types work correctly
    String postgresType = testObject.boolField.getDDLType(DatabaseType.POSTGRES);
    assertEquals("BOOLEAN", postgresType);

    String mysqlType = testObject.boolField.getDDLType(DatabaseType.MYSQL);
    assertEquals("tinyint", mysqlType);
  }

  @Test
  public void testBooleanFieldIsChanged() {
    assertFalse(testObject.boolField.isChanged());

    testObject.boolField.changeValue(true);
    assertTrue(testObject.boolField.isChanged());
  }

  @Test
  public void testBooleanFieldWithDefaultValuePostgres() {
    String defaultValue = testObject.boolWithDefault.getDefaultValue(DatabaseType.POSTGRES);
    assertEquals("true", defaultValue);
  }

  @Test
  public void testChangeValueUnlessToNullBehaviorWithBoolean() {
    testObject.boolField.changeValue(true);
    testObject.boolField.changeValueUnlessToNull(null);
    // Should NOT change to null (because changeValueUnlessToNull rejects null)
    assertEquals(true, testObject.boolField.getValue());

    testObject.boolField.changeValueUnlessToNull(false);
    // Should change to false
    assertEquals(false, testObject.boolField.getValue());
  }

  @Test
  public void testInitializeValueDoesNotMarkAsChanged() {
    testObject.boolField.initializeValue(true);
    assertFalse(testObject.boolField.isChanged());
    assertEquals(true, testObject.boolField.getValue());
  }

  @Test
  public void testNullValueHelperMethod() {
    testObject.nullableBoolField.changeValue(true);
    assertTrue(testObject.nullableBoolField.getValue());
    testObject.nullableBoolField.updateInternal(); // Make true the "original" value

    testObject.nullableBoolField.nullValue();
    assertNull(testObject.nullableBoolField.getValue());
    // nullValue() sets changedValue to null, making it different from originalValue (true)
    assertTrue(testObject.nullableBoolField.isChanged());
  }

  @Test
  public void testFieldNameIsCorrect() {
    assertEquals("bool_field", testObject.boolField.getFieldName());
    assertEquals("nullable_bool_field", testObject.nullableBoolField.getFieldName());
  }
}
