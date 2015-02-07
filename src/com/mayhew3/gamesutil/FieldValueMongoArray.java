package com.mayhew3.gamesutil;

import com.mongodb.BasicDBList;
import org.bson.types.ObjectId;

public class FieldValueMongoArray extends FieldValue<BasicDBList> {
  public FieldValueMongoArray(String fieldName, FieldConversion<BasicDBList> converter) {
    super(fieldName, converter);
  }

  // todo: update code should really use $addToSet instead of sending the whole array in every time.
  public void addToArray(ObjectId value) {
    BasicDBList objectIds = getValue();
    if (!objectIds.contains(value)) {
      objectIds.add(value);
    }
  }

  @Override
  protected void initializeValue(BasicDBList value) {
    super.initializeValue(value);
  }

  @Override
  protected void initializeValueFromString(String valueString) {
    super.initializeValueFromString(valueString);
  }

  public void removeFromArray(ObjectId value) {
    BasicDBList objectIds = getValue();
    objectIds.remove(value);
  }
}
