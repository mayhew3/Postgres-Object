package com.mayhew3.gamesutil.tv;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.sql.Timestamp;

interface TVDBJWTProvider {

  JSONObject findSeriesMatches(String formattedTitle) throws UnirestException;

  JSONObject getSeriesData(Integer tvdbSeriesId) throws UnirestException;

  JSONObject getEpisodeSummaries(Integer tvdbSeriesId, Integer pageNumber) throws UnirestException;

  JSONObject getEpisodeData(Integer tvdbEpisodeId) throws UnirestException;

  JSONObject getPosterData(Integer tvdbId) throws UnirestException;

  JSONObject getUpdatedSeries(Timestamp fromDate) throws UnirestException;
}
