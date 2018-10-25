package com.mayhew3.mediamogul.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface JSONReader {

  @NotNull
  JSONObject getObjectWithKey(JSONObject jsonObject, String key);

  @NotNull
  JSONArray getArrayWithKey(JSONObject jsonObject, String key);

  @NotNull
  String getStringWithKey(JSONObject jsonObject, String key);

  @Nullable
  String getNullableStringWithKey(JSONObject jsonObject, String key);

  @NotNull
  Integer getIntegerWithKey(JSONObject jsonObject, String key);

  @Nullable
  Integer getNullableIntegerWithKey(JSONObject jsonObject, String key);

  @Nullable
  Double getNullableDoubleWithKey(JSONObject jsonObject, String key);

  void forEach(JSONArray jsonArray, Consumer<JSONObject> jsonObjectConsumer);

  @NotNull
  List<JSONObject> findMatches(JSONArray jsonArray, Predicate<JSONObject> conditionToLookFor);

  @NotNull
  JSONObject parseJSONObject(String filepath);

  @NotNull
  JSONArray parseJSONArray(String filepath);

  @NotNull
  Optional<JSONObject> getOptionalObjectWithKey(JSONObject jsonObject, String key);
}
