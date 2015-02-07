package com.mayhew3.gamesutil.mediaobject;

import com.mongodb.*;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class MediaObject {

  private enum EditMode {INSERT, UPDATE}

  private EditMode editMode;

  List<FieldValue> allFieldValues = new ArrayList<>();

  public FieldValue<ObjectId> _id = new FieldValue<>("_id", new FieldConversionMongoId());
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
    }
  }

  private void insert(DB db) {
    BasicDBObject insertObject = new BasicDBObject();

    List<FieldValue> changedFields = new ArrayList<>();

    dateAdded.changeValue(new Date());
    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.getOriginalValue() != null) {
        throw new IllegalStateException("Shouldn't find any original values on Insert object.");
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
  }

  private void update(DB db) {
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

  protected final FieldValue<BasicDBList> registerStringArrayField(String fieldName) {
    FieldValue<BasicDBList> fieldStringArrayValue = new FieldValue<>(fieldName, new FieldConversionStringArray());
    allFieldValues.add(fieldStringArrayValue);
    return fieldStringArrayValue;
  }

  protected final FieldValue<Boolean> registerBooleanField(String fieldName) {
    FieldValue<Boolean> fieldBooleanValue = new FieldValueBoolean(fieldName, new FieldConversionBoolean());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValue<Date> registerDateField(String fieldName) {
    FieldValue<Date> fieldBooleanValue = new FieldValue<>(fieldName, new FieldConversionDate());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValue<Integer> registerIntegerField(String fieldName) {
    FieldValue<Integer> fieldIntegerValue = new FieldValue<>(fieldName, new FieldConversionInteger());
    allFieldValues.add(fieldIntegerValue);
    return fieldIntegerValue;
  }

  protected final FieldValue<ObjectId> registerObjectIdField(String fieldName) {
    FieldValue<ObjectId> fieldMongoValue = new FieldValue<>(fieldName, new FieldConversionMongoId());
    allFieldValues.add(fieldMongoValue);
    return fieldMongoValue;
  }

  protected final FieldValueMongoArray registerMongoArrayField(String fieldName) {
    FieldValueMongoArray fieldMongoArray = new FieldValueMongoArray(fieldName, new FieldConversionMongoArray());
    allFieldValues.add(fieldMongoArray);
    return fieldMongoArray;
  }

  protected final FieldValue<Double> registerDoubleField(String fieldName) {
    FieldValue<Double> fieldDoubleValue = new FieldValue<>(fieldName, new FieldConversionDouble());
    allFieldValues.add(fieldDoubleValue);
    return fieldDoubleValue;
  }

  protected final FieldValue<String> registerStringField(String fieldName) {
    FieldValue<String> fieldBooleanValue = new FieldValueString(fieldName, new FieldConversionString());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

}
