package com.mayhew3.gamesutil.mediaobjectmongo;

public class FieldConversionString extends FieldConversion<String> {
  @Override
  String parseFromString(String value) {
    return value;
  }
}
