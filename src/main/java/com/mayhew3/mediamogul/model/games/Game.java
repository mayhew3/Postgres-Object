package com.mayhew3.mediamogul.model.games;

import com.mayhew3.mediamogul.dataobject.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Game extends DataObject {

  public FieldValue<Timestamp> finished = registerTimestampField("finished", Nullability.NULLABLE);
  public FieldValue<Timestamp> added = registerTimestampField("added", Nullability.NULLABLE);
  public FieldValue<Timestamp> giantbomb_release_date = registerTimestampField("giantbomb_release_date", Nullability.NULLABLE);
  public FieldValue<Timestamp> howlong_updated = registerTimestampField("howlong_updated", Nullability.NULLABLE);
  public FieldValue<Timestamp> howlong_failed = registerTimestampField("howlong_failed", Nullability.NULLABLE);
  public FieldValue<Timestamp> lastPlayed = registerTimestampField("last_played", Nullability.NULLABLE);

  public FieldValue<BigDecimal> metacritic = registerBigDecimalField("metacritic", Nullability.NULLABLE);
  public FieldValue<BigDecimal> guess = registerBigDecimalField("guess", Nullability.NULLABLE);
  public FieldValue<BigDecimal> mayhew = registerBigDecimalField("mayhew", Nullability.NULLABLE);
  public FieldValue<BigDecimal> sum = registerBigDecimalField("sum", Nullability.NULLABLE);
  public FieldValue<BigDecimal> sumd = registerBigDecimalField("sumd", Nullability.NULLABLE);
  public FieldValue<BigDecimal> timeplayed = registerBigDecimalField("timeplayed", Nullability.NULLABLE);
  public FieldValue<BigDecimal> timetotal = registerBigDecimalField("timetotal", Nullability.NULLABLE);
  public FieldValue<BigDecimal> percentDone = registerBigDecimalField("percentdone", Nullability.NULLABLE);
  public FieldValue<BigDecimal> remaining = registerBigDecimalField("remaining", Nullability.NULLABLE);
  public FieldValue<BigDecimal> percent = registerBigDecimalField("percent", Nullability.NULLABLE);
  public FieldValue<BigDecimal> percentd = registerBigDecimalField("percentd", Nullability.NULLABLE);
  public FieldValue<BigDecimal> playtime = registerBigDecimalField("playtime", Nullability.NULLABLE);
  public FieldValue<BigDecimal> finalscore = registerBigDecimalField("finalscore", Nullability.NULLABLE);
  public FieldValue<BigDecimal> replay = registerBigDecimalField("replay", Nullability.NULLABLE);
  public FieldValue<BigDecimal> adj = registerBigDecimalField("adj", Nullability.NULLABLE);
  public FieldValue<BigDecimal> remainder = registerBigDecimalField("remainder", Nullability.NULLABLE);
  public FieldValue<BigDecimal> howlong_main = registerBigDecimalField("howlong_main", Nullability.NULLABLE);
  public FieldValue<BigDecimal> howlong_extras = registerBigDecimalField("howlong_extras", Nullability.NULLABLE);
  public FieldValue<BigDecimal> howlong_completionist = registerBigDecimalField("howlong_completionist", Nullability.NULLABLE);
  public FieldValue<BigDecimal> howlong_all = registerBigDecimalField("howlong_all", Nullability.NULLABLE);



  public FieldValueInteger total = registerIntegerField("total", Nullability.NULLABLE);
  public FieldValueInteger totalinc = registerIntegerField("totalinc", Nullability.NULLABLE);
  public FieldValueInteger die1 = registerIntegerField("die1", Nullability.NULLABLE);
  public FieldValueInteger die2 = registerIntegerField("die2", Nullability.NULLABLE);
  public FieldValueInteger finaldie = registerIntegerField("finaldie", Nullability.NULLABLE);
  public FieldValueInteger steam_attribute_count = registerIntegerField("steam_attribute_count", Nullability.NULLABLE);

  public FieldValueInteger steamID = registerIntegerField("steamid", Nullability.NULLABLE);
  public FieldValueInteger giantbomb_id = registerIntegerField("giantbomb_id", Nullability.NULLABLE);
  public FieldValueInteger giantbomb_year = registerIntegerField("giantbomb_year", Nullability.NULLABLE);
  public FieldValueInteger howlong_id = registerIntegerField("howlong_id", Nullability.NULLABLE);
  public FieldValueInteger howlong_main_confidence = registerIntegerField("howlong_main_confidence", Nullability.NULLABLE);
  public FieldValueInteger howlong_extras_confidence = registerIntegerField("howlong_extras_confidence", Nullability.NULLABLE);
  public FieldValueInteger howlong_completionist_confidence = registerIntegerField("howlong_completionist_confidence", Nullability.NULLABLE);
  public FieldValueInteger howlong_all_confidence = registerIntegerField("howlong_all_confidence", Nullability.NULLABLE);
  public FieldValueInteger year = registerIntegerField("year", Nullability.NULLABLE);

  public FieldValueString title = registerStringField("title", Nullability.NOT_NULL);
  public FieldValueString platform = registerStringField("platform", Nullability.NOT_NULL);
  public FieldValueString owned = registerStringField("owned", Nullability.NULLABLE);
  public FieldValueString by = registerStringField("by", Nullability.NULLABLE);
  public FieldValueString reason = registerStringField("reason", Nullability.NULLABLE);
  public FieldValueString icon = registerStringField("icon", Nullability.NULLABLE);
  public FieldValueString logo = registerStringField("logo", Nullability.NULLABLE);

  public FieldValueString giantbomb_name = registerStringField("giantbomb_name", Nullability.NULLABLE);
  public FieldValueString giantbomb_icon_url = registerStringField("giantbomb_icon_url", Nullability.NULLABLE);
  public FieldValueString giantbomb_medium_url = registerStringField("giantbomb_medium_url", Nullability.NULLABLE);
  public FieldValueString giantbomb_screen_url = registerStringField("giantbomb_screen_url", Nullability.NULLABLE);
  public FieldValueString giantbomb_small_url = registerStringField("giantbomb_small_url", Nullability.NULLABLE);
  public FieldValueString giantbomb_super_url = registerStringField("giantbomb_super_url", Nullability.NULLABLE);
  public FieldValueString giantbomb_thumb_url = registerStringField("giantbomb_thumb_url", Nullability.NULLABLE);
  public FieldValueString giantbomb_tiny_url = registerStringField("giantbomb_tiny_url", Nullability.NULLABLE);
  public FieldValueString giantbomb_best_guess = registerStringField("giantbomb_best_guess", Nullability.NULLABLE);
  public FieldValueString giantbomb_manual_guess = registerStringField("giantbomb_manual_guess", Nullability.NULLABLE);
  public FieldValueString howlong_title = registerStringField("howlong_title", Nullability.NULLABLE);


  public FieldValueBoolean giantbomb_guess_confirmed = registerBooleanFieldAllowingNulls("giantbomb_guess_confirmed", Nullability.NULLABLE);

  public FieldValueString metacriticHint = registerStringField("metacritic_hint", Nullability.NULLABLE);
  public FieldValueBoolean metacriticPage = registerBooleanField("metacritic_page", Nullability.NOT_NULL).defaultValue(false);
  public FieldValueBoolean steam_cloud = registerBooleanFieldAllowingNulls("steam_cloud", Nullability.NULLABLE);
  public FieldValueBoolean steam_controller = registerBooleanFieldAllowingNulls("steam_controller", Nullability.NULLABLE);
  public FieldValueBoolean steam_local_coop = registerBooleanFieldAllowingNulls("steam_local_coop", Nullability.NULLABLE);

  public FieldValue<Timestamp> metacriticMatched = registerTimestampField("metacritic_matched", Nullability.NULLABLE);
  public FieldValue<Timestamp> steam_attributes = registerTimestampField("steam_attributes", Nullability.NULLABLE);
  public FieldValue<Timestamp> steam_page_gone = registerTimestampField("steam_page_gone", Nullability.NULLABLE);

  public FieldValueBoolean started = registerBooleanFieldAllowingNulls("started", Nullability.NULLABLE);
  public FieldValueBoolean include = registerBooleanFieldAllowingNulls("include", Nullability.NULLABLE);
  public FieldValueBoolean naturalEnd = registerBooleanField("natural_end", Nullability.NOT_NULL).defaultValue(true);

  @Override
  public String getTableName() {
    return "game";
  }

  @Override
  public String toString() {
    String msg = title.getValue() + " (" + id.getValue() + ")";
    Integer steamID = this.steamID.getValue();
    if (steamID != null) {
      msg += " (Steam: " + steamID + ")";
    }
    return msg;
  }
}
