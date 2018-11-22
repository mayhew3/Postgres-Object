package com.mayhew3.mediamogul.model.tv.group;

import com.mayhew3.mediamogul.dataobject.Nullability;
import com.mayhew3.mediamogul.dataobject.RetireableDataObject;

public class TVGroupBallot extends RetireableDataObject {

  public TVGroupBallot() {
    registerTimestampField("voting_open", Nullability.NOT_NULL).defaultValueNow();
    registerTimestampField("voting_closed", Nullability.NULLABLE);
    registerStringField("reason", Nullability.NULLABLE);
    registerForeignKey(new TVGroupSeries(), Nullability.NOT_NULL);
    registerIntegerField("last_episode", Nullability.NULLABLE);
    registerIntegerField("first_episode", Nullability.NULLABLE);
  }

  @Override
  public String getTableName() {
    return "tv_group_ballot";
  }
}
