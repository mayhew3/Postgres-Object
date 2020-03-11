package com.mayhew3.postgresobject.dataobject;

public class TmpDraftAverages extends DataObject {

  public FieldValueString player = registerStringField("Player", Nullability.NULLABLE);
  public FieldValueDate statDate = registerDateField("StatDate", Nullability.NULLABLE);

  public FieldValueBigDecimal avg_pick = registerBigDecimalField("Avg Pick", Nullability.NULLABLE);
  public FieldValueBigDecimal percent_drafted = registerBigDecimalField("% Drafted", Nullability.NULLABLE);
  public FieldValueString hi_lo = registerStringField("HI/LO", Nullability.NULLABLE);
  public FieldValueInteger playerID = registerIntegerField("PlayerID", Nullability.NULLABLE);
  public FieldValueInteger rank = registerIntegerField("Rank", Nullability.NULLABLE);

  @Override
  public String getTableName() {
    return "cbs_draftaverages";
  }
}
