package com.mayhew3.gamesutil.mediaobject;

import org.bson.types.ObjectId;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FieldValueMongoObjectId extends FieldValue<ObjectId> {
  public FieldValueMongoObjectId(String fieldName, FieldConversion<ObjectId> converter) {
    super(fieldName, converter);
  }

  @Override
  protected void initializeValue(ResultSet resultSet) {
    throw new IllegalStateException("Cannot select Postgres DB with Mongo value.");
  }

  @Override
  public void updatePreparedStatement(PreparedStatement preparedStatement, int currentIndex) {
    throw new IllegalStateException("Cannot update Postgres DB with Mongo value.");
  }
}
