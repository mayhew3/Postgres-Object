package com.mayhew3.mediamogul.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

public class JSONReaderImpl implements JSONReader {

  @Override
  public @NotNull JSONObject getObjectWithKey(JSONObject jsonObject, String key) {
    return jsonObject.getJSONObject(key);
  }

  @Override
  public @Nullable JSONObject getNullableObjectWithKey(JSONObject jsonObject, String key) {
    if (!jsonObject.has(key) || JSONObject.NULL.equals(jsonObject.get(key))) {
      return null;
    } else {
      return jsonObject.getJSONObject(key);
    }
  }

  @Override
  public @NotNull JSONArray getArrayWithKey(JSONObject jsonObject, String key) {
    if (!jsonObject.has(key) || JSONObject.NULL.equals(jsonObject.get(key))) {
      return new JSONArray();
    } else {
      return jsonObject.getJSONArray(key);
    }
  }

  @Override
  public @NotNull String getStringWithKey(JSONObject jsonObject, String key) {
    return jsonObject.getString(key);
  }

  @Override
  public @Nullable String getNullableStringWithKey(JSONObject jsonObject, String key) {
    if (!jsonObject.has(key) || JSONObject.NULL.equals(jsonObject.get(key))) {
      return null;
    } else {
      String objectString = jsonObject.getString(key);
      if ("".equals(objectString)) {
        return null;
      } else {
        return objectString;
      }
    }
  }

  @Override
  public @NotNull Integer getIntegerWithKey(JSONObject jsonObject, String key) {
    return jsonObject.getInt(key);
  }

  @Override
  public @Nullable Integer getNullableIntegerWithKey(JSONObject jsonObject, String key) {
    if (!jsonObject.has(key) || JSONObject.NULL.equals(jsonObject.get(key))) {
      return null;
    } else {
      return jsonObject.getInt(key);
    }
  }

  @Override
  public @Nullable Double getNullableDoubleWithKey(JSONObject jsonObject, String key) {
    if (!jsonObject.has(key) || JSONObject.NULL.equals(jsonObject.get(key))) {
      return null;
    } else {
      return jsonObject.getDouble(key);
    }
  }
}
