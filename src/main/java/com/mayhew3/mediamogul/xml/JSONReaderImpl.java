package com.mayhew3.mediamogul.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class JSONReaderImpl implements JSONReader {

  @Override
  public @NotNull JSONObject getObjectWithKey(JSONObject jsonObject, String key) {
    return jsonObject.getJSONObject(key);
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

  @Override
  public void forEach(JSONArray jsonArray, Consumer<JSONObject> jsonObjectConsumer) {
    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject jsonObject = jsonArray.getJSONObject(i);
      jsonObjectConsumer.accept(jsonObject);
    }
  }

  @NotNull
  @Override
  public List<JSONObject> findMatches(JSONArray jsonArray, Predicate<JSONObject> conditionToLookFor) {
    List<JSONObject> matches = new ArrayList<>();
    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject jsonObject = jsonArray.getJSONObject(i);
      if (conditionToLookFor.test(jsonObject)) {
        matches.add(jsonObject);
      }
    }
    return matches;
  }


  @NotNull
  @Override
  public JSONArray parseJSONArray(String filepath) {
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(filepath));
      String text = new String(bytes, Charset.defaultCharset());
      return new JSONArray(text);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Unable to read from file path: " + filepath);
    }
  }

}
