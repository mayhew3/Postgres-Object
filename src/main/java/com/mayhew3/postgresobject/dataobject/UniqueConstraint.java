package com.mayhew3.postgresobject.dataobject;

import java.util.ArrayList;
import java.util.List;

class UniqueConstraint {

  private List<FieldValue> fields;

  UniqueConstraint(ArrayList<FieldValue> fieldValues) {
    this.fields = fieldValues;
  }

  List<FieldValue> getFields() {
    return fields;
  }
}
