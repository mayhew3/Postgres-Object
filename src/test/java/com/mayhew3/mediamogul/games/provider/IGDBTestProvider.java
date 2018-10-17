package com.mayhew3.mediamogul.games.provider;

import com.mayhew3.mediamogul.xml.JSONReader;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.util.function.Consumer;

public class IGDBTestProvider implements IGDBProvider {
  private String filePrefix;
  private JSONReader jsonReader;

  public IGDBTestProvider(String filePrefix, JSONReader jsonReader) {
    this.filePrefix = filePrefix;
    this.jsonReader = jsonReader;
  }

  @Override
  public JSONArray findGameMatches(String gameTitle) {
    String filepath = filePrefix + "search_" + gameTitle + ".json";
    @NotNull JSONArray jsonArrayFromFile = jsonReader.parseJSONArray(filepath);
     return jsonArrayFromFile;
  }

}
