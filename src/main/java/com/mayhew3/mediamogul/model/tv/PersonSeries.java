package com.mayhew3.mediamogul.model.tv;

import com.mayhew3.mediamogul.dataobject.*;
import com.mayhew3.mediamogul.model.Person;

public class PersonSeries extends RetireableDataObject {

  FieldValueForeignKey seriesId = registerForeignKey(new Series(), Nullability.NOT_NULL);
  FieldValueForeignKey personId = registerForeignKey(new Person(), Nullability.NOT_NULL);

  FieldValueInteger rating = registerIntegerField("rating", Nullability.NULLABLE);
  FieldValueInteger tier = registerIntegerField("tier", Nullability.NOT_NULL).defaultValue(1);

  FieldValueBoolean disavowed = registerBooleanField("disavowed", Nullability.NOT_NULL).defaultValue(false);


  PersonSeries() {
    addUniqueConstraint(seriesId, personId);
  }

  @Override
  public String getTableName() {
    return "person_series";
  }

  @Override
  public String toString() {
    return "Person " + personId.getValue() + ", Series " + seriesId.getValue();
  }

}
