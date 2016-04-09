package com.mayhew3.gamesutil.dataobject;

import com.mongodb.BasicDBList;
import org.bson.types.ObjectId;

public class FieldConversionMongoArray extends FieldConversion<BasicDBList> {

  @Override
  BasicDBList parseFromString(String value) {
    if (value == null) {
      return null;
    }
    String[] split = value.split(",");
    BasicDBList dbList = new BasicDBList();
    for (String objStr : split) {
      dbList.add(new ObjectId(objStr));
    }
    return dbList;
  }
}
