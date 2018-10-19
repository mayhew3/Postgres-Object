package com.mayhew3.mediamogul.games.provider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.function.Consumer;

public interface IGDBProvider {

  JSONArray findGameMatches(String gameTitle);

  JSONObject getUpdatedInfo(Integer igdb_id);
}
