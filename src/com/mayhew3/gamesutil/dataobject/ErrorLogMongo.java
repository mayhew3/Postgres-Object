package com.mayhew3.gamesutil.dataobject;

public class ErrorLogMongo extends MediaObjectMongoDB {

  /* Data */
  public FieldValue<String> chosenName = registerStringField("ChosenName");
  public FieldValue<String> errorMessage = registerStringField("ErrorMessage");
  public FieldValue<String> errorType = registerStringField("ErrorType");
  public FieldValueDate eventDate = registerDateField("EventDate");
  public FieldValue<String> formattedName = registerStringField("FormattedName");
  public FieldValueBoolean resolved = registerBooleanField("Resolved");
  public FieldValueDate resolvedDate = registerDateField("ResolvedDate");
  public FieldValue<String> tvdbName = registerStringField("TVDBName");
  public FieldValue<String> tivoId = registerStringField("TiVoID");
  public FieldValue<String> tivoName = registerStringField("TiVoName");
  public FieldValue<String> context = registerStringField("Context");
  public FieldValueBoolean ignoreError = registerBooleanField("IgnoreError");

  @Override
  protected String getTableName() {
    return "series";
  }

  @Override
  public String toString() {
    return "Error type '" + errorType.getValue() + "', Message: " + errorMessage.getValue();
  }

}
