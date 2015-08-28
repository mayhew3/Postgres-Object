package com.mayhew3.gamesutil.mediaobject;

public class FieldConversionShort extends FieldConversion<Short> {
  @Override
  Short parseFromString(String value) {
    if (value == null) {
      return null;
    }
    return Short.valueOf(value);
  }
}
