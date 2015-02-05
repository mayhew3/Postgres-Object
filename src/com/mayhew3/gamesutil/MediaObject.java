package com.mayhew3.gamesutil;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class MediaObject {

  List<FieldValue> allFieldValues = new ArrayList<>();

  FieldValue<ObjectId> _id = new FieldValue<>("_id", new FieldConversionMongoId());

  public void initializeFromDBObject(DBObject dbObject) {

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

  private void initializeValue(FieldValue fieldValue, Object obj) {
    if (obj instanceof String) {
      fieldValue.initializeValueFromString((String) obj);
    } else {
      fieldValue.initializeValue(obj);
    }
  }

  public void commit(DB db) {
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

  public void markFieldsForUpgrade() {
    for (FieldValue fieldValue : allFieldValues) {
      if (fieldValue.shouldUpgradeText()) {
        fieldValue.changeValue(fieldValue.getOriginalValue());
      }
    }
  }

  protected abstract String getTableName();

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

  protected final FieldValue<String> registerStringField(String fieldName) {
    FieldValue<String> fieldBooleanValue = new FieldValueString(fieldName, new FieldConversionString());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }
}
