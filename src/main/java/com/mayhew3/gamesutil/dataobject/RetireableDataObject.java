package com.mayhew3.gamesutil.dataobject;

import java.sql.Timestamp;
import java.util.Date;

public abstract class RetireableDataObject extends DataObject {
  public FieldValueInteger retired = registerIntegerField("retired", Nullability.NOT_NULL).defaultValue(0);
  public FieldValueTimestamp retiredDate = registerTimestampField("retired_date", Nullability.NULLABLE);

  public void retire() {
    retired.changeValue(id.getValue());
    retiredDate.changeValue(new Timestamp(new Date().getTime()));
  }
}

