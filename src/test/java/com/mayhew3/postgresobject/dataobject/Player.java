package com.mayhew3.postgresobject.dataobject;

public class Player extends DataObject {

  public FieldValueInteger cbs_id = registerIntegerField("CBS_ID", Nullability.NULLABLE);
  public FieldValueString playerString = registerStringField("PlayerString", Nullability.NOT_NULL);
  public FieldValueString newPlayerString = registerStringField("NewPlayerString", Nullability.NULLABLE);
  public FieldValueString firstName = registerStringField("FirstName", Nullability.NULLABLE);
  public FieldValueString lastName = registerStringField("LastName", Nullability.NULLABLE);
  public FieldValueString mlbTeam = registerStringField("MLBTeam", Nullability.NULLABLE);
  public FieldValueString position = registerStringField("Position", Nullability.NULLABLE);
  public FieldValueString eligibility = registerStringField("Eligibility", Nullability.NULLABLE);
  public FieldValueString injury = registerStringField("Injury", Nullability.NULLABLE);
  public FieldValueTimestamp updateTime = registerTimestampField("UpdateTime", Nullability.NULLABLE);
  public FieldValueBoolean matchPending = registerBooleanField("MatchPending", Nullability.NULLABLE).defaultValue(false);

  @Override
  public String getTableName() {
    return "players";
  }
}
