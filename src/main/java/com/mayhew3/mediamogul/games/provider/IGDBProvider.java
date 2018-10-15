package com.mayhew3.mediamogul.games.provider;

import org.json.JSONArray;

import java.util.function.Consumer;

public interface IGDBProvider {

  void findGameMatches(String gameTitle, Consumer<JSONArray> resultHandler);

}
