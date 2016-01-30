package com.mayhew3.gamesutil.mediaobject;

import com.mongodb.*;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class MediaObjectMongoDB {

  private enum EditMode {INSERT, UPDATE}

  private EditMode editMode;

  List<FieldValue> allFieldValues = new ArrayList<>();

  public FieldValue<ObjectId> _id = new FieldValueMongoObjectId("_id", new FieldConversionMongoId());
  public FieldValue<Date> dateAdded = registerDateField("DateAdded");

  public void initializeFromDBObject(DBObject dbObject) {
    editMode = EditMode.UPDATE;

    Object existingId = dbObject.get("_id");

    if (existingId == null) {
      throw new RuntimeException("DBObject found with no valid _id field.");
    }

    initializeValue(_id, existingId);

    for (FieldValue fieldValue : allFieldValues) {
      String fieldName = fieldValue.getFieldName();
      Object obj = dbObject.get(fieldName);

      initializeValue(fieldValue, obj);
    }
  }

  public void initializeForInsert() {
    editMode = EditMode.INSERT;
  }

  public void changeToUpdateObject() {
    editMode = EditMode.UPDATE;
  }

  private void initializeValue(FieldValue fieldValue, Object obj) {
    if (obj instanceof String) {
      fieldValue.initializeValueFromString((String) obj);
    } else {
      // todo: Need to change MongoArray fieldvalue to BasicDBList instead of ObjectId[]. Probably need the same for genre.
      fieldValue.initializeValue(obj);
    }
  }

  public void commit(DB db) {
    if (editMode == EditMode.UPDATE) {
      update(db);
    } else if (editMode == EditMode.INSERT) {
      insert(db);
    } else {
      throw new IllegalStateException("Attempting to commit MediaObjectMongoDB that wasn't properly initialized!");
    }
  }

  private void insert(DB db) {
    BasicDBObject insertObject = new BasicDBObject();

    List<FieldValue> changedFields = new ArrayList<>();

    dateAdded.changeValue(new Date());
    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.getOriginalValue() != null) {
        throw new IllegalStateException("Shouldn't find any original values on Insert object: '" + fieldValue.getFieldName() + "' field has value '"
            + fieldValue.getOriginalValue() + "', changed value '" + fieldValue.getChangedValue() + "'");
      }
      if (fieldValue.isChanged()) {
        insertObject.append(fieldValue.getFieldName(), fieldValue.getChangedValue());
        changedFields.add(fieldValue);
      }
    }

    if (!insertObject.isEmpty()) {
      insertIntoDatabase(db, insertObject);
      updateObjects(changedFields);
      _id.initializeValue((ObjectId) insertObject.get("_id"));
    }

    // INSERT COMPLETE. Subsequent changes should be updates.
    editMode = EditMode.UPDATE;
  }

  private void update(DB db) {
    if (_id == null) {
      throw new RuntimeException("Cannot update object with no _id field.");
    }
    if (_id.isChanged()) {
      throw new RuntimeException("Cannot change _id field on existing object.");
    }
    BasicDBObject queryObject = new BasicDBObject("_id", _id.getValue());
    BasicDBObject updateObject = new BasicDBObject();

    List<FieldValue> changedFields = new ArrayList<>();

    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.isChanged()) {
        updateObject.append(fieldValue.getFieldName(), fieldValue.getChangedValue());
        changedFields.add(fieldValue);
      }
    }
    if (!updateObject.isEmpty()) {
      updateDatabase(db, queryObject, updateObject);
      updateObjects(changedFields);
    }
  }

  private void updateObjects(List<FieldValue> changedFields) {
    for (FieldValue changedField : changedFields) {
      changedField.updateInternal();
    }
  }

  private WriteResult updateDatabase(DB db, BasicDBObject queryObject, BasicDBObject updateObject) {
    return db.getCollection(getTableName()).update(queryObject, new BasicDBObject("$set", updateObject));
  }

  private WriteResult insertIntoDatabase(DB db, BasicDBObject updateObject) {
    return db.getCollection(getTableName()).insert(updateObject);
  }

  public void markFieldsForUpgrade() {
    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.shouldUpgradeText()) {
        fieldValue.changeValue(fieldValue.getOriginalValue());
      }
    }
  }

  protected abstract String getTableName();

  public List<FieldValue> getAllFieldValues() {
    return allFieldValues;
  }

  public FieldValue getMatchingField(FieldValue matchingValue) {
    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.getFieldName().equals(matchingValue.getFieldName())) {
        return fieldValue;
      }
    }
    return null;
  }


  protected final FieldValue<BasicDBList> registerStringArrayField(String fieldName) {
    FieldValue<BasicDBList> fieldStringArrayValue = new FieldValueMongoArray(fieldName, new FieldConversionStringArray());
    allFieldValues.add(fieldStringArrayValue);
    return fieldStringArrayValue;
  }

  protected final FieldValueDBObjectArray registerIntegerDBObjectArrayField(String fieldName) {
    FieldValueDBObjectArray fieldIntegerArrayValue = new FieldValueDBObjectArray(fieldName, new FieldConversionIntegerDBObjectArray());
    allFieldValues.add(fieldIntegerArrayValue);
    return fieldIntegerArrayValue;
  }

  protected final FieldValueBoolean registerBooleanField(String fieldName) {
    FieldValueBoolean fieldBooleanValue = new FieldValueBoolean(fieldName, new FieldConversionBoolean());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValueDate registerDateField(String fieldName) {
    FieldValueDate fieldBooleanValue = new FieldValueDate(fieldName, new FieldConversionDate());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValueInteger registerIntegerField(String fieldName) {
    FieldValueInteger fieldIntegerValue = new FieldValueInteger(fieldName, new FieldConversionInteger());
    allFieldValues.add(fieldIntegerValue);
    return fieldIntegerValue;
  }

  protected final FieldValue<ObjectId> registerObjectIdField(String fieldName) {
    FieldValue<ObjectId> fieldMongoValue = new FieldValueMongoObjectId(fieldName, new FieldConversionMongoId());
    allFieldValues.add(fieldMongoValue);
    return fieldMongoValue;
  }

  protected final FieldValueMongoArray registerMongoArrayField(String fieldName) {
    FieldValueMongoArray fieldMongoArray = new FieldValueMongoArray(fieldName, new FieldConversionMongoArray());
    allFieldValues.add(fieldMongoArray);
    return fieldMongoArray;
  }

  protected final FieldValue<Double> registerDoubleField(String fieldName) {
    FieldValue<Double> fieldDoubleValue = new FieldValueDouble(fieldName, new FieldConversionDouble());
    allFieldValues.add(fieldDoubleValue);
    return fieldDoubleValue;
  }

  protected final FieldValue<String> registerStringField(String fieldName) {
    FieldValue<String> fieldBooleanValue = new FieldValueString(fieldName, new FieldConversionString());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

}
