package com.mayhew3.gamesutil.mediaobjectmongo;

import org.bson.types.ObjectId;

public class FieldConversionMongoId extends FieldConversion<ObjectId> {
  @Override
  ObjectId parseFromString(String value) {
    return value == null ? null : new ObjectId(value);
  }
}
