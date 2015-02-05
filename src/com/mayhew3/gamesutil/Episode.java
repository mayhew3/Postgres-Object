package com.mayhew3.gamesutil;

import java.util.Date;

public class Episode extends MediaObject {

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

  @Override
  protected String getTableName() {
    return "episodes";
  }
}
