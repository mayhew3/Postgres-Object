package com.mayhew3.mediamogul.dataobject;

public class FieldConversionString extends FieldConversion<String> {
  @Override
  String parseFromString(String value) {
    return value;
  }
}
