package com.mayhew3.postgresobject.dataobject;

public class CBSId extends DataObject {

  public FieldValueInteger cbs_id = registerIntegerField("cbs_id", Nullability.NOT_NULL);
  public FieldValueString playerString = registerStringField("PlayerString", Nullability.NOT_NULL);
  public FieldValueString injuryNote = registerStringField("InjuryNote", Nullability.NULLABLE);
  public FieldValueDate dateAdded = registerDateField("DateAdded", Nullability.NULLABLE);
  public FieldValueDate dateModified = registerDateField("DateModified", Nullability.NULLABLE);
  public FieldValueInteger year = registerIntegerField("year", Nullability.NULLABLE);

  @Override
  public String getTableName() {
    return "cbsids";
  }
}
