package com.mayhew3.gamesutil.mediaobject;

public class FieldConversionInteger extends FieldConversion<Integer> {
  @Override
  Integer parseFromString(String value) {
    if (value == null) {
      return null;
    }
    return Integer.valueOf(value);
  }
}
