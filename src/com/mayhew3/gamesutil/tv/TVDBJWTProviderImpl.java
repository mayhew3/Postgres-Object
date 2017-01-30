package com.mayhew3.gamesutil.tv;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;


class TVDBJWTProviderImpl implements TVDBJWTProvider {
  private String token = null;

  TVDBJWTProviderImpl() throws UnirestException {
    if (token == null) {
      token = getToken();
    }
  }


  @Override
  public JSONObject findSeriesMatches(String formattedTitle) throws UnirestException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/search/series";

    Map<String, Object> queryParams = Maps.newHashMap();
    queryParams.put("name", formattedTitle);

    HttpResponse<JsonNode> response = getData(seriesUrl, queryParams);
    JsonNode body = response.getBody();
    return body.getObject();
  }

  @Override
  public JSONObject getSeriesData(Integer tvdbSeriesId) throws UnirestException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/series/" + tvdbSeriesId;

    HttpResponse<JsonNode> response = getData(seriesUrl);
    JsonNode body = response.getBody();
    return body.getObject();
  }

  @Override
  public JSONObject getEpisodeSummaries(Integer tvdbSeriesId, Integer pageNumber) throws UnirestException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/series/" + tvdbSeriesId + "/episodes";

    Map<String, Object> queryParams = Maps.newHashMap();
    queryParams.put("page", pageNumber);

    HttpResponse<JsonNode> response = getData(seriesUrl, queryParams);
    JsonNode body = response.getBody();
    return body.getObject();
  }

  @Override
  public JSONObject getEpisodeData(Integer tvdbEpisodeId) throws UnirestException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/episodes/" + tvdbEpisodeId;

    HttpResponse<JsonNode> response = getData(seriesUrl);
    JsonNode body = response.getBody();
    return body.getObject();
  }

  @Override
  public JSONObject getPosterData(Integer tvdbId) throws UnirestException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/series/" + tvdbId + "/images/query";

    Map<String, Object> queryParams = Maps.newHashMap();
    queryParams.put("keyType", "poster");

    HttpResponse<JsonNode> response = getData(seriesUrl, queryParams);
    JsonNode body = response.getBody();
    return body.getObject();
  }

  @Override
  public JSONObject getUpdatedSeries(Timestamp fromDate) throws UnirestException {
    Preconditions.checkState(token != null);

    long epochTime = getEpochTime(fromDate);

    System.out.println("Epoch time: " + epochTime);

    String seriesUrl = "https://api.thetvdb.com/updated/query";

    Map<String, Object> queryParams = Maps.newHashMap();
    queryParams.put("fromTime", epochTime);

    HttpResponse<JsonNode> response = getData(seriesUrl, queryParams);

    System.out.println("Response: " + response.getStatusText());

    JsonNode body = response.getBody();
    return body.getObject();
  }

  public long getEpochTime(Timestamp fromDate) {
    return fromDate.getTime() / 1000L;
  }

  void writeSearchToFile(String formattedTitle) throws UnirestException, IOException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/search/series";

    Map<String, Object> queryParams = Maps.newHashMap();
    queryParams.put("name", formattedTitle);

    JSONObject jsonObject = getData(seriesUrl, queryParams).getBody().getObject();

    String filePath = "resources\\TVDBTest\\search_" + formattedTitle + ".json";

    writeResultToFile(filePath, jsonObject);
  }

  void writeSeriesToFile(Integer tvdbId) throws UnirestException, IOException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/series/" + tvdbId;

    JSONObject jsonObject = getData(seriesUrl).getBody().getObject();

    String filePath = "resources\\TVDBTest\\" + tvdbId + "_summary.json";

    writeResultToFile(filePath, jsonObject);
  }

  void writePostersToFile(Integer tvdbSeriesId) throws UnirestException, IOException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/series/" + tvdbSeriesId + "/images/query";

    Map<String, Object> queryParams = Maps.newHashMap();
    queryParams.put("keyType", "poster");

    JsonNode body = getData(seriesUrl, queryParams).getBody();
    JSONObject jsonObject = body.getObject();

    String filePath = "resources\\TVDBTest\\" + tvdbSeriesId + "_posters.json";

    writeResultToFile(filePath, jsonObject);
  }


  void writeEpisodeDetailsToFiles(Integer tvdbSeriesId, List<Pair<Integer, Integer>> episodeNumbers) throws IOException, UnirestException {
    for (Pair<Integer, Integer> episodeNumber : episodeNumbers) {
      writeEpisodeDetailToFile(tvdbSeriesId, episodeNumber.getKey(), episodeNumber.getValue());
    }
  }

  private void writeEpisodeDetailToFile(Integer tvdbSeriesId, Integer seasonNumber, Integer episodeNumber) throws UnirestException, IOException {
    Preconditions.checkState(token != null);

    String seriesUrl = "https://api.thetvdb.com/series/" + tvdbSeriesId + "/episodes/query";
    Map<String, Object> params = Maps.newHashMap();
    params.put("airedSeason", seasonNumber);
    params.put("airedEpisode", episodeNumber);

    JSONObject jsonObject = getData(seriesUrl, params).getBody().getObject();

    JSONArray jsonArray = jsonObject.getJSONArray("data");
    JSONObject episodeSummary = jsonArray.getJSONObject(0);

    int tvdbEpisodeId = episodeSummary.getInt("id");

    String episodeUrl = "https://api.thetvdb.com/episodes/" + tvdbEpisodeId;

    JSONObject episodeObject = getData(episodeUrl).getBody().getObject();

    String filePath = "resources\\TVDBTest\\" + "E" + tvdbEpisodeId + ".json";
    writeResultToFile(filePath, episodeObject);
  }


  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void writeResultToFile(String localFilePath, JSONObject jsonObject) throws IOException {
    File file = new File(localFilePath);

    if (!file.exists()) {
      file.createNewFile();
    }

    FileWriter fileWriter = new FileWriter(file);
    fileWriter.write(jsonObject.toString(2));
    fileWriter.close();
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
