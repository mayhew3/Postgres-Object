package com.mayhew3.gamesutil.mediaobjectmongo;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;

public class FieldValueDBObjectArray extends FieldValue<BasicDBList> {
  public FieldValueDBObjectArray(String fieldName, FieldConversion<BasicDBList> converter) {
    super(fieldName, converter);
  }

  // todo: update code should really use $addToSet instead of sending the whole array in every time.
  public void addToArray(DBObject dbObject, String indexFieldName) {
    BasicDBList objectIds = getValue();

    Object indexValue = dbObject.get(indexFieldName);

    DBObject existingObjectWithKey = findExistingObjectWithKey(indexFieldName, indexValue);

    BasicDBList dbList = new BasicDBList();
    if (objectIds != null) {
      dbList.addAll(objectIds);
    }
    if (existingObjectWithKey != null) {
      dbList.remove(existingObjectWithKey);
    }

    dbList.add(dbObject);
    changeValue(dbList);
  }

  private DBObject findExistingObjectWithKey(String keyFieldName, Object keyFieldValue) {
    BasicDBList objects = getValue();

    if (keyFieldValue == null || objects == null) {
      return null;
    }

    for (Object object : objects) {
      DBObject dbObject = (DBObject) object;
      if (keyFieldValue.equals(dbObject.get(keyFieldName))) {
        return dbObject;
      }
    }
    return null;
  }

  @Override
  protected void initializeValue(BasicDBList value) {
    super.initializeValue(value);
  }

  @Override
  protected void initializeValueFromString(String valueString) {
    super.initializeValueFromString(valueString);
  }

  public void removeFromArray(DBObject value) {
    BasicDBList objectIds = getValue();
    BasicDBList dbList = new BasicDBList();
    dbList.addAll(objectIds);
    dbList.remove(value);
    changeValue(dbList);
  }
}
