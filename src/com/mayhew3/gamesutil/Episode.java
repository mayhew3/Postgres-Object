package com.mayhew3.gamesutil;

import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Episode {

  List<FieldValue> allFieldValues = new ArrayList<>();

  FieldValue<Date> DATE_ADDED = registerDateField("DateAdded");
  FieldValue<Date> WATCHED_DATE = registerDateField("WatchedDate");

  FieldValue<Date> TIVO_SHOWING_START_TIME = registerDateField("TiVoShowingStartTime");
  FieldValue<Date> TIVO_DELETED_DATE = registerDateField("TiVoDeletedDate");
  FieldValue<Date> TIVO_CAPTURE_DATE = registerDateField("TiVoCaptureDate");

  FieldValue<Date> TVDB_FIRST_AIRED = registerDateField("tvdbFirstAired");

  FieldValue<String> TivoEpisodeTitle = registerStringField("TivoEpisodeTitle");



  FieldValue<Boolean> ON_TIVO = registerBooleanField("OnTiVo");
  FieldValue<Boolean> WATCHED = registerBooleanField("Watched");
  FieldValue<Boolean> MATCHED_STUMP = registerBooleanField("MatchedStump");


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

  protected final FieldValue<String> registerStringField(String fieldName) {
    FieldValue<String> fieldBooleanValue = new FieldValue<>(fieldName, new FieldConversionString());
    allFieldValues.add(fieldBooleanValue);
    return fieldBooleanValue;
  }
}
