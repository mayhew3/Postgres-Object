package com.mayhew3.postgresobject.dataobject;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class UniqueConstraint {

  private List<FieldValue> fields;
  private String tableName;

  UniqueConstraint(ArrayList<FieldValue> fieldValues, String tableName) {
    this.fields = fieldValues;
    this.tableName = tableName;
  }

  String getIndexName() {
    List<String> fieldNames = getFields().stream()
        .map(FieldValue::getFieldName)
        .collect(Collectors.toList());
    String underJoin = Joiner.on("_").join(fieldNames);
    return tableName + "_" + underJoin + "_key";
  }

  List<FieldValue> getFields() {
    return fields;
  }
}
