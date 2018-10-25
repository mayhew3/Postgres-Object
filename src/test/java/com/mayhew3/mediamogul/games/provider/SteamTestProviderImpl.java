package com.mayhew3.mediamogul.games.provider;

import com.mayhew3.mediamogul.xml.JSONReader;
import org.json.JSONObject;

public class SteamTestProviderImpl implements SteamProvider {

  private String fileHeader;
  private JSONReader jsonReader;
  private String fileSuffix;

  public SteamTestProviderImpl(String fileHeader, JSONReader jsonReader) {
    this.fileHeader = fileHeader;
    this.jsonReader = jsonReader;
  }

  @Override
  public JSONObject getSteamInfo() {
    assert fileSuffix != null;
    return jsonReader.parseJSONObject(fileHeader + fileSuffix + ".json");
  }

  @Override
  public String getFullUrl() {
    throw new UnsupportedOperationException("No URL for tests.");
  }

  public void setFileSuffix(String suffix) {
    this.fileSuffix = suffix;
  }
}
