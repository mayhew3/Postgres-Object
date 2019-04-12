package com.mayhew3.postgresobject.dataobject;

import java.util.ArrayList;
import java.util.List;

class ColumnsIndex {

  private List<FieldValue> fields;

  ColumnsIndex(ArrayList<FieldValue> fieldValues) {
    this.fields = fieldValues;
  }

  List<FieldValue> getFields() {
    return fields;
  }
}
