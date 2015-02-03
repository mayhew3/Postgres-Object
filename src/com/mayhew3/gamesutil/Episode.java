package com.mayhew3.gamesutil;

import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Episode {

  List<FieldValue> allFieldValues = new ArrayList<>();

  FieldValue<Date> dateAdded = registerDateField("DateAdded");
  FieldValue<Date> watchedDate = registerDateField("WatchedDate");

  FieldValue<Date> tiVoShowingStartTime = registerDateField("TiVoShowingStartTime");
  FieldValue<Date> tiVoDeletedDate = registerDateField("TiVoDeletedDate");
  FieldValue<Date> tiVoCaptureDate = registerDateField("TiVoCaptureDate");

  FieldValue<Date> tvdbFirstAired = registerDateField("tvdbFirstAired");

  FieldValue<String> tivoEpisodeTitle = registerStringField("TivoEpisodeTitle");
  FieldValue<String> tvdbEpisodeId = registerStringField("tvdbEpisodeId");

  FieldValue<Boolean> onTiVo = registerBooleanField("OnTiVo");
  FieldValue<Boolean> watched = registerBooleanField("Watched");
  FieldValue<Boolean> tiVoSuggestion = registerBooleanField("TiVoSuggestion");
  FieldValue<Boolean> matchedStump = registerBooleanField("MatchedStump");


  public Episode(DBObject dbObject) {
    for (FieldValue fieldValue : allFieldValues) {
      Object obj = dbObject.get(fieldValue.getFieldName());
      if (obj instanceof String) {
        fieldValue.setValueFromString((String) obj);
      } else {
        fieldValue.setValue(obj);
      }
    }
  }

  protected final <G> FieldValue<G> registerField(String fieldName, FieldConversion<G> converter) {
    FieldValue<G> fieldValue = new FieldValue<>(fieldName, converter);
    allFieldValues.add(fieldValue);
    return fieldValue;
  }

  protected final FieldValue<Boolean> registerBooleanField(String fieldName) {
    FieldValue<Boolean> fieldBooleanValue = new FieldValueBoolean(fieldName, new FieldConversionBoolean());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValue<Date> registerDateField(String fieldName) {
    FieldValue<Date> fieldBooleanValue = new FieldValue<>(fieldName, new FieldConversionDate());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }

  protected final FieldValue<Integer> registerIntegerField(String fieldName) {
    FieldValue<Integer> fieldIntegerValue = new FieldValue<>(fieldName, new FieldConversionInteger());
    allFieldValues.add(fieldIntegerValue);
    return fieldIntegerValue;
  }

  protected final FieldValue<String> registerStringField(String fieldName) {
    FieldValue<String> fieldBooleanValue = new FieldValue<>(fieldName, new FieldConversionString());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }
}
