package com.mayhew3.gamesutil.dataobject;

public class FieldConversionBoolean extends FieldConversion<Boolean> {
  @Override
  Boolean parseFromString(String value) {
    return value == null ? false : Boolean.valueOf(value);
  }
}
