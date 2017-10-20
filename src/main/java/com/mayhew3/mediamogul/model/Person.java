package com.mayhew3.mediamogul.model;

import com.mayhew3.mediamogul.dataobject.FieldValueString;
import com.mayhew3.mediamogul.dataobject.Nullability;
import com.mayhew3.mediamogul.dataobject.RetireableDataObject;

public class Person extends RetireableDataObject {

  public FieldValueString username = registerStringField("username", Nullability.NOT_NULL);
  public FieldValueString email = registerStringField("email", Nullability.NOT_NULL);
  public FieldValueString firstName = registerStringField("first_name", Nullability.NOT_NULL);
  public FieldValueString lastName = registerStringField("last_name", Nullability.NOT_NULL);

  public Person() {
    addUniqueConstraint(username);
    addUniqueConstraint(email);
  }

  @Override
  public String getTableName() {
    return "person";
  }

  @Override
  public String toString() {
    return email.getValue();
  }

}
