package com.mayhew3.gamesutil.tv;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.io.FileNotFoundException;

interface TVDBJWTProvider {

  JSONObject findSeriesMatches(String formattedTitle) throws UnirestException;

  JSONObject getSeriesData(Integer tvdbId, String subpath) throws UnirestException;

  JSONObject getEpisodeData(Integer tvdbEpisodeId) throws UnirestException;

  JSONObject getPosterData(Integer tvdbId) throws UnirestException;
}
