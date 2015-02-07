package com.mayhew3.gamesutil;

import com.mongodb.BasicDBList;

import java.util.Collections;

public class FieldConversionStringArray extends FieldConversion<BasicDBList> {
  @Override
  BasicDBList parseFromString(String value) {
    String[] split = value.replaceFirst("^\\|", "")
        .split("\\|");
    BasicDBList objects = new BasicDBList();

    Collections.addAll(objects, split);

    return objects;
  }
}
