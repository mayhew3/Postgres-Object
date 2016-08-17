package com.mayhew3.gamesutil.tv;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import org.json.JSONObject;

import java.util.Map;

class TVDBJWTProvider {
  private String token = null;

//  private String localFilePath = null;

  TVDBJWTProvider() throws UnirestException {
    if (token == null) {
      token = getToken();
    }
  }

//  public TVDBJWTProvider(String localFilePath) {
//    this.localFilePath = localFilePath;
//  }

  JSONObject findSeriesMatches(String formattedTitle) throws UnirestException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/search/series";

    Map<String, Object> queryParams = Maps.newHashMap();
    queryParams.put("name", formattedTitle);

    HttpResponse<JsonNode> response = getData(seriesUrl, queryParams);
    JsonNode body = response.getBody();
    return body.getObject();
  }

  JSONObject getSeriesData(Integer tvdbId, String subpath) throws UnirestException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/series/" + tvdbId + subpath;

    HttpResponse<JsonNode> response = getData(seriesUrl);
    JsonNode body = response.getBody();
    return body.getObject();
  }

  JSONObject getEpisodeData(Integer tvdbEpisodeId) throws UnirestException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/episodes/" + tvdbEpisodeId;

    HttpResponse<JsonNode> response = getData(seriesUrl);
    JsonNode body = response.getBody();
    return body.getObject();
  }


  private String getToken() throws UnirestException {
    String tvdbApiKey = System.getenv("TVDB_API_KEY");
    if (tvdbApiKey == null) {
      throw new IllegalStateException("No TVDB_API_KEY environment variable found!");
    }

    String urlString = "https://api.thetvdb.com/login";
    HttpRequest httpRequest = Unirest.post(urlString)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .body(new JSONObject().put("apikey", tvdbApiKey))
        .getHttpRequest();
    HttpResponse<JsonNode> response = httpRequest
        .asJson();
    JsonNode body = response.getBody();
    JSONObject object = body.getObject();
    return object.getString("token");
  }

  private HttpResponse<JsonNode> getData(String url, Map<String, Object> queryParams) throws UnirestException {
    return Unirest.get(url)
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .header("Authorization", "Bearer " + token)
          .header("Accept-Language", "en")
          .queryString(queryParams)
          .asJson();
  }

  private HttpResponse<JsonNode> getData(String url) throws UnirestException {
    return getData(url, Maps.newHashMap());
  }
}
