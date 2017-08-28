package com.mayhew3.mediamogul.dataobject;

import java.math.BigDecimal;

public class FieldConversionBigDecimal extends FieldConversion<BigDecimal> {
  @Override
  BigDecimal parseFromString(String value) {
    return (value == null || "".equals(value)) ? null : new BigDecimal(value);
  }
}
