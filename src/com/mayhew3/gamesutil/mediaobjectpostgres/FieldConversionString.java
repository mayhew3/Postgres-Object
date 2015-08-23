package com.mayhew3.gamesutil.mediaobjectpostgres;

public class FieldConversionString extends FieldConversion<String> {
  @Override
  String parseFromString(String value) {
    return value;
  }
}
