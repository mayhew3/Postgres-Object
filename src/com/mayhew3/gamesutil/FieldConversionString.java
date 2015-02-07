package com.mayhew3.gamesutil;

public class FieldConversionString extends FieldConversion<String> {
  @Override
  String parseFromString(String value) {
    return value;
  }
}
