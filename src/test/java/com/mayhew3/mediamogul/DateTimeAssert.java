package com.mayhew3.mediamogul;

import org.fest.assertions.api.AbstractAssert;
import org.fest.assertions.api.Assertions;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;

public class DateTimeAssert extends AbstractAssert<DateTimeAssert, DateTime> {

  public DateTimeAssert(DateTime actual) {
    super(actual, DateTimeAssert.class);
  }

  public static DateTimeAssert assertThat(DateTime actual) {
    return new DateTimeAssert(actual);
  }

  public DateTimeAssert isAfter(DateTime otherDateTime) {
    isNotNull();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    Assertions.assertThat(actual.isAfter(otherDateTime))
        .overridingErrorMessage("Expected datetime <%s> to be after <%s>",
            sdf.format(actual.toDate()),
            sdf.format(otherDateTime.toDate())
        )
        .isTrue();
    return this;
  }
}
