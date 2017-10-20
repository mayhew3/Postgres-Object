package com.mayhew3.mediamogul.model;

import com.mayhew3.mediamogul.dataobject.FieldValueForeignKey;
import com.mayhew3.mediamogul.dataobject.FieldValueString;
import com.mayhew3.mediamogul.dataobject.Nullability;
import com.mayhew3.mediamogul.dataobject.RetireableDataObject;

public class PersonRole extends RetireableDataObject {

  // foreign keys
  FieldValueForeignKey personId = registerForeignKey(new Person(), Nullability.NOT_NULL);

  FieldValueString role = registerStringField("role", Nullability.NOT_NULL);

  public PersonRole() {
    addUniqueConstraint(personId, role);
  }

  @Override
  public String getTableName() {
    return "person_role";
  }

  @Override
  public String toString() {
    return personId.getValue() + ": " + role.getValue();
  }

}
