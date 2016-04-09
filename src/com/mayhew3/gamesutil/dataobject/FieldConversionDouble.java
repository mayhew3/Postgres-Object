package com.mayhew3.gamesutil.dataobject;

public class FieldConversionDouble extends FieldConversion<Double> {
  @Override
  Double parseFromString(String value) {
    return value == null ? null : Double.valueOf(value);
  }
}
