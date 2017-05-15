package com.mayhew3.gamesutil.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

public interface JSONReader {

  @NotNull
  JSONObject getObjectWithKey(JSONObject jsonObject, String key);

  @Nullable
  JSONObject getNullableObjectWithKey(JSONObject jsonObject, String key);

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

}
