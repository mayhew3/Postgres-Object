package com.mayhew3.gamesutil.mediaobject;

import java.math.BigDecimal;

public class FieldValueBigDecimal extends FieldValue<BigDecimal> {
  public FieldValueBigDecimal(String fieldName, FieldConversion<BigDecimal> converter) {
    super(fieldName, converter);
  }

  public void changeValue(Double newValue) {
    if (newValue == null) {
      changeValue((BigDecimal) null);
    } else {
      changeValue(BigDecimal.valueOf(newValue));
    }
  }
}
