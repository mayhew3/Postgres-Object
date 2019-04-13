package com.mayhew3.postgresobject.dataobject;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class ColumnsIndex {

  private List<FieldValue> fields;
  private String tableName;
  private Integer order;

  ColumnsIndex(ArrayList<FieldValue> fieldValues, String tableName, Integer order) {
    this.fields = fieldValues;
    this.tableName = tableName;
    this.order = order;
  }

  String getIndexName() {
    List<String> fieldNames = getFields().stream().map(FieldValue::getFieldName).collect(Collectors.toList());
    String underJoin = Joiner.on("_").join(fieldNames);
    return tableName + "_" + underJoin + "_ix" + order;
  }

  List<FieldValue> getFields() {
    return fields;
  }
}
