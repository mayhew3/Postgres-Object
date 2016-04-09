package com.mayhew3.gamesutil.dataobject;

import com.mongodb.BasicDBList;

public class FieldConversionIntegerDBObjectArray extends FieldConversion<BasicDBList> {
  @Override
  BasicDBList parseFromString(String value) {
    String[] split = value.replaceFirst("^\\|", "")
        .split("\\|");
    BasicDBList objects = new BasicDBList();

    for (String stringPiece : split) {
      Integer integer = Integer.valueOf(stringPiece);
      objects.add(integer);
    }

    return objects;
  }
}
