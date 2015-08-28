package com.mayhew3.gamesutil.mediaobject;

import com.mayhew3.gamesutil.mediaobject.FieldConversion;
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
