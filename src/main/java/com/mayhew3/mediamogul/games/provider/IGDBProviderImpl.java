package com.mayhew3.mediamogul.games.provider;

import callback.OnSuccessCallback;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import wrapper.IGDBWrapper;
import wrapper.Parameters;
import wrapper.Version;

import java.util.function.Consumer;

public class IGDBProviderImpl implements IGDBProvider {
  private IGDBWrapper igdbWrapper;

  public IGDBProviderImpl() {
    this.igdbWrapper = new IGDBWrapper(System.getenv("igdb_key"), Version.STANDARD, false);
  }

  @Override
  public void findGameMatches(String gameTitle, Consumer<JSONArray> resultHandler) {
    Parameters parameters = new Parameters()
        .addSearch(gameTitle)
        .addFields("name,cover")
        .addLimit("5")
        .addOffset("0");

    igdbWrapper.games(parameters, new OnSuccessCallback() {
      @Override
      public void onSuccess(@NotNull JSONArray jsonArray) {
        resultHandler.accept(jsonArray);
        System.out.println(" - Finished code path.");
      }

      @Override
      public void onError(@NotNull Exception e) {
        throw new RuntimeException(e);
      }
    });

  }

}
