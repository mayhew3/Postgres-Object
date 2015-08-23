package com.mayhew3.gamesutil.mediaobjectmongo;

public class FieldConversionDouble extends FieldConversion<Double> {
  @Override
  Double parseFromString(String value) {
    return value == null ? null : Double.valueOf(value);
  }
}
