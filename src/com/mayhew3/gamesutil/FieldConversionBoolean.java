package com.mayhew3.gamesutil;

public class FieldConversionBoolean extends FieldConversion<Boolean> {
  @Override
  Boolean setValue(String value) {
    return value == null ? false : Boolean.valueOf(value);
  }
}
