package com.mayhew3.gamesutil.dataobject;

public abstract class RetireableDataObject extends DataObject {
  public FieldValueInteger retired = registerIntegerField("retired", Nullability.NOT_NULL).defaultValue(0);
  public FieldValueTimestamp retiredDate = registerTimestampField("retired_date", Nullability.NULLABLE);
}

