package com.mayhew3.mediamogul.model.tv.group;

import com.mayhew3.mediamogul.dataobject.Nullability;
import com.mayhew3.mediamogul.dataobject.RetireableDataObject;
import com.mayhew3.mediamogul.model.Person;

public class TVGroupVote extends RetireableDataObject {

  public TVGroupVote() {
    registerForeignKey(new TVGroupBallot(), Nullability.NOT_NULL);
    registerForeignKey(new Person(), Nullability.NOT_NULL);
    registerIntegerField("vote_value", Nullability.NULLABLE);
    registerBooleanField("vote_skipped", Nullability.NOT_NULL).defaultValue(false);
  }

  @Override
  public String getTableName() {
    return "tv_group_vote";
  }
}
