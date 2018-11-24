package com.mayhew3.mediamogul.model.tv.group;

import com.mayhew3.mediamogul.dataobject.FieldValueForeignKey;
import com.mayhew3.mediamogul.dataobject.FieldValueInteger;
import com.mayhew3.mediamogul.dataobject.Nullability;
import com.mayhew3.mediamogul.dataobject.RetireableDataObject;
import com.mayhew3.mediamogul.model.Person;

public class TVGroupVote extends RetireableDataObject {
  public FieldValueForeignKey ballot_id = registerForeignKey(new TVGroupBallot(), Nullability.NOT_NULL);
  public FieldValueForeignKey person_id = registerForeignKey(new Person(), Nullability.NOT_NULL);

  public FieldValueInteger vote_value = registerIntegerField("vote_value", Nullability.NULLABLE);

  public TVGroupVote() {
    registerBooleanField("vote_skipped", Nullability.NOT_NULL).defaultValue(false);
    addUniqueConstraint(ballot_id, person_id, retired);
  }

  @Override
  public String getTableName() {
    return "tv_group_vote";
  }
}
