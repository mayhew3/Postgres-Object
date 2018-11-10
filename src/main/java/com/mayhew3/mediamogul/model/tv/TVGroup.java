package com.mayhew3.mediamogul.model.tv;

import com.mayhew3.mediamogul.dataobject.FieldValueString;
import com.mayhew3.mediamogul.dataobject.Nullability;
import com.mayhew3.mediamogul.dataobject.RetireableDataObject;

public class TVGroup extends RetireableDataObject {

  private FieldValueString name = registerStringField("name", Nullability.NULLABLE);

  public TVGroup() {
    addUniqueConstraint(name);
  }

  @Override
  public String getTableName() {
    return "tv_group";
  }

  @Override
  public String toString() {
    return name.getValue() + ": ID " + id.getValue();
  }

}
