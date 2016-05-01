package com.mayhew3.gamesutil.dataobject;

import java.util.ArrayList;
import java.util.List;

public class UniqueConstraint {

  private List<FieldValue> fields;

  public UniqueConstraint() {
    this.fields = new ArrayList<>();
  }

  public UniqueConstraint addField(FieldValue fieldValue) {
    fields.add(fieldValue);
    return this;
  }


  public List<FieldValue> getFields() {
    return fields;
  }

}
