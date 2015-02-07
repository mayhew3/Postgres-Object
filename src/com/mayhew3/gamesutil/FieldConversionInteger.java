package com.mayhew3.gamesutil;

public class FieldConversionInteger extends FieldConversion<Integer> {
  @Override
  Integer parseFromString(String value) {
    return value == null ? null : Integer.valueOf(value);
  }
}
