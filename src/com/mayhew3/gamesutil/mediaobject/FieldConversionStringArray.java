package com.mayhew3.gamesutil.mediaobject;

import com.mongodb.BasicDBList;

import java.util.Collections;

public class FieldConversionStringArray extends FieldConversion<BasicDBList> {
  @Override
  BasicDBList parseFromString(String value) {
    BasicDBList objects = new BasicDBList();

    if (value != null) {
      String[] split = value.replaceFirst("^\\|", "")
          .split("\\|");

      Collections.addAll(objects, split);
    }

    return objects;
  }
}
