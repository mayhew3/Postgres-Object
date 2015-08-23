package com.mayhew3.gamesutil.mediaobjectpostgres;

import java.util.Date;

public class FieldValueDate extends FieldValue<Date> {
  public FieldValueDate(String fieldName, FieldConversion<Date> converter) {
    super(fieldName, converter);
  }

  public void changeValueFromXMLString(String xmlString) {
    if (xmlString != null) {
      long numberOfSeconds = Long.decode(xmlString);
      changeValue(new Date(numberOfSeconds * 1000));
    }
  }
}
