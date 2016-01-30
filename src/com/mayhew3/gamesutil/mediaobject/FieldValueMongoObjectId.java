package com.mayhew3.gamesutil.mediaobject;

import org.bson.types.ObjectId;

import java.sql.PreparedStatement;

public class FieldValueMongoObjectId extends FieldValue<ObjectId> {
  public FieldValueMongoObjectId(String fieldName, FieldConversion<ObjectId> converter) {
    super(fieldName, converter);
  }

  @Override
  public void updatePreparedStatement(PreparedStatement preparedStatement, int currentIndex) {
    throw new IllegalStateException("Cannot update Postgres DB with Mongo value.");
  }
}
