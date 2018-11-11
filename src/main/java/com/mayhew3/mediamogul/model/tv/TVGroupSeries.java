package com.mayhew3.mediamogul.model.tv;

import com.mayhew3.mediamogul.dataobject.FieldValueForeignKey;
import com.mayhew3.mediamogul.dataobject.Nullability;
import com.mayhew3.mediamogul.dataobject.RetireableDataObject;

public class TVGroupSeries extends RetireableDataObject {

  /* Data */
  private FieldValueForeignKey tv_group_id = registerForeignKey(new TVGroup(), Nullability.NOT_NULL);
  private FieldValueForeignKey series_id = registerForeignKey(new Series(), Nullability.NOT_NULL);

  public TVGroupSeries() {
    addUniqueConstraint(tv_group_id, series_id);
  }

  @Override
  public String getTableName() {
    return "tv_group_series";
  }

  @Override
  public String toString() {
    return "tv_group_series " + tv_group_id.getValue() + ", " + series_id.getValue();
  }

}
