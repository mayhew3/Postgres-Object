package com.mayhew3.gamesutil.mediaobject;

import com.mayhew3.gamesutil.mediaobject.FieldConversion;
import org.bson.types.ObjectId;

public class FieldConversionMongoId extends FieldConversion<ObjectId> {
  @Override
  ObjectId parseFromString(String value) {
    return value == null ? null : new ObjectId(value);
  }
}
