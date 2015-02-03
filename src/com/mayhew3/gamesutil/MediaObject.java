package com.mayhew3.gamesutil;

import com.mongodb.DBObject;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class MediaObject {

  List<FieldValue> allFieldValues = new ArrayList<>();

  FieldValue<ObjectId> _id = registerObjectIdField("_id");

  public MediaObject(DBObject dbObject) {
    for (FieldValue fieldValue : allFieldValues) {
      String fieldName = fieldValue.getFieldName();
      Object obj = dbObject.get(fieldName);

      if ("_id".equals(fieldName) && obj == null) {
        throw new RuntimeException("DBObject found with no valid _id field.");
      }

      if (obj instanceof String) {
        fieldValue.setValueFromString((String) obj);
      } else {
        fieldValue.setValue(obj);
      }
    }
  }

  public Object getValueByFieldName(String fieldName) {
    if (fieldName == null) {
      return null;
    }
    for (FieldValue fieldValue : allFieldValues) {
      if (fieldName.equals(fieldValue.getFieldName())) {
        return fieldValue.getValue();
      }
    }
    return null;
  }

  protected final <G> FieldValue<G> registerField(String fieldName, FieldConversion<G> converter) {
    FieldValue<G> fieldValue = new FieldValue<>(fieldName, converter);
    allFieldValues.add(fieldValue);
    return fieldValue;
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
    FieldValue<ObjectId> fieldObjectIdValue = new FieldValue<>(fieldName, new FieldConversionMongoId());
    allFieldValues.add(fieldObjectIdValue);
    return fieldObjectIdValue;
  }

  protected final FieldValue<String> registerStringField(String fieldName) {
    FieldValue<String> fieldBooleanValue = new FieldValue<>(fieldName, new FieldConversionString());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }
}
