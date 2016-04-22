package com.mayhew3.gamesutil.dataobject;

public class ErrorLog extends DataObject {

  /* Data */
  public FieldValueString chosenName = registerStringField("chosen_name");
  public FieldValueString errorMessage = registerStringField("error_message");
  public FieldValueString errorType = registerStringField("error_type");
  public FieldValueTimestamp eventDate = registerTimestampField("event_date");
  public FieldValueString formattedName = registerStringField("formatted_name");
  public FieldValueBoolean resolved = registerBooleanField("resolved");
  public FieldValueTimestamp resolvedDate = registerTimestampField("resolved_date");
  public FieldValueString tvdbName = registerStringField("tvdb_name");
  public FieldValueString tivoId = registerStringField("tivo_id");
  public FieldValueString tivoName = registerStringField("tivo_name");
  public FieldValueString context = registerStringField("context");
  public FieldValueBoolean ignoreError = registerBooleanField("ignore_error");

  @Override
  protected String getTableName() {
    return "error_log";
  }

  @Override
  public String toString() {
    return "Error type '" + errorType.getValue() + "', Message: " + errorMessage.getValue();
  }

}
