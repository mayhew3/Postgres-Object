package com.mayhew3.gamesutil;

public class FieldConversionInteger extends FieldConversion<Integer> {
  @Override
  Integer setValue(String value) {
    return value == null ? null : Integer.valueOf(value);
  }
}
