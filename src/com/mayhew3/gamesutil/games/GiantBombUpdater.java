package com.mayhew3.gamesutil.games;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.mediaobject.Game;
import com.mayhew3.gamesutil.mediaobject.GameLog;
import com.sun.istack.internal.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GiantBombUpdater {

  private static PostgresConnection connection;

  public static void main(String[] args) throws SQLException, FileNotFoundException {
    List<String> argList = Lists.newArrayList(args);
    Boolean logToFile = argList.contains("LogToFile");

    if (logToFile) {
      File file = new File("D:\\Projects\\mean_projects\\GamesDBUtil\\logs\\SteamUpdaterErrors.log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);

      System.err.println("Starting run on " + new Date());
    }

    connection = new PostgresConnection();
    updateFields();

  }

  public static void updateFields() throws SQLException {
    String sql = "SELECT * FROM games WHERE giantbomb_name IS NULL and owned <> 'not owned'";
    ResultSet resultSet1 = connection.executeQuery(sql);

    while (connection.hasMoreElements(resultSet1)) {
      Game game = new Game();
      game.initializeFromDBObject(resultSet1);

      String title = game.title.getValue();
      Integer giantbomb_id = game.giantbomb_id.getValue();
      String giantbomb_title = title;
      if (game.giantbomb_best_guess.getValue() != null && game.giantbomb_guess_confirmed.getValue()) {
        giantbomb_title = game.giantbomb_best_guess.getValue();
      }

      try {
        JSONArray jsonArray = null;
        JSONObject match;
        if (giantbomb_id == null) {
          jsonArray = getResultsArray(giantbomb_title);
          match = findMatch(jsonArray, giantbomb_title);
        } else {
          match = getSingleGameWithId(giantbomb_id);
        }

        if (match != null) {

          debug("O) " + title + ": Match found.");

          String name = match.getString("name");

          try {
            JSONObject image = match.getJSONObject("image");

            game.giantbomb_name.changeValue(name);

            if (giantbomb_id == null) {
              game.giantbomb_id.changeValue(match.getInt("id"));
            }

            String original_release_date = match.getString("original_release_date");

            if (original_release_date != null) {
              SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
              Date timestamp = simpleDateFormat.parse(original_release_date);
              int year = timestamp.getYear() + 1900;
              game.giantbomb_year.changeValue(year);
            }

            game.giantbomb_icon_url.changeValue(image.getString("icon_url"));
            game.giantbomb_medium_url.changeValue(image.getString("medium_url"));
            game.giantbomb_screen_url.changeValue(image.getString("screen_url"));
            game.giantbomb_small_url.changeValue(image.getString("small_url"));
            game.giantbomb_super_url.changeValue(image.getString("super_url"));
            game.giantbomb_thumb_url.changeValue(image.getString("thumb_url"));
            game.giantbomb_tiny_url.changeValue(image.getString("tiny_url"));

            game.commit(connection);
          } catch (JSONException e) {
            debug("Error getting image object for results on '" + title + "'.");
            e.printStackTrace();
          } catch (ParseException e) {
            debug("Error parsing date.");
            e.printStackTrace();
          }

        } else {
          debug("X) " + title + ": No match found!");

          if (game.giantbomb_manual_guess.getValue() != null) {
            JSONArray lessRestrictive = getResultsArray(game.giantbomb_manual_guess.getValue());
            JSONObject anotherBestGuess = getBestGuess(lessRestrictive, game.giantbomb_manual_guess.getValue(), game);

            if (anotherBestGuess != null) {
              game.giantbomb_best_guess.changeValue(anotherBestGuess.getString("name"));
              game.commit(connection);
            }
          } else {
            JSONObject bestGuess = getBestGuess(jsonArray, title, game);

            if (bestGuess != null) {
              game.giantbomb_best_guess.changeValue(bestGuess.getString("name"));
              game.commit(connection);
            }
          }
        }

//      debug(jsonObject);
      } catch (IOException e) {
        debug("Error reading from URL.");
        e.printStackTrace();
      }




    }

    debug("Operation finished!");

  }

  private static JSONArray getResultsArray(String title) throws IOException {
    String fullURL = getFullUrl(title);
    JSONObject jsonObject = readJsonFromUrl(fullURL);
    return jsonObject.getJSONArray("results");
  }

  @Nullable
  private static JSONObject findMatch(JSONArray parentArray, String title) throws SQLException {

    for (int i = 0; i < parentArray.length(); i++) {
      JSONObject jsonGame = parentArray.getJSONObject(i);

      String name = jsonGame.getString("name");

      if (title.equalsIgnoreCase(name)) {
        return jsonGame;
      }
    }

    return null;
  }

  @Nullable
  private static JSONObject getBestGuess(JSONArray parentArray, String title, Game game) throws SQLException {
    String previousBestGuess = game.giantbomb_best_guess.getValue();

    Boolean confirmed = game.giantbomb_guess_confirmed.getValue();

    if (previousBestGuess != null) {
      if (confirmed != null && !confirmed) {
        Boolean foundPreviousGuess = false;
        for (int i = 0; i < parentArray.length(); i++) {
          JSONObject jsonGame = parentArray.getJSONObject(i);
          String name = jsonGame.getString("name");
          if (foundPreviousGuess) {
            return jsonGame;
          } else {
            if (name.equalsIgnoreCase(previousBestGuess)) {
              foundPreviousGuess = true;
            }
          }
        }
        return null;
      } else {
        return null;
      }
    }

    if (parentArray.length() == 0) {
      return null;
    } else {
      return parentArray.getJSONObject(0);
    }
  }

  private static void logUpdateToPlaytime(String name, Integer steamID, BigDecimal previousPlaytime, BigDecimal updatedPlaytime) {
    GameLog gameLog = new GameLog();
    gameLog.initializeForInsert();

    gameLog.game.changeValue(name);
    gameLog.steamID.changeValue(steamID);
    gameLog.platform.changeValue("Steam");
    gameLog.previousPlaytime.changeValue(previousPlaytime);
    gameLog.updatedplaytime.changeValue(updatedPlaytime);
    gameLog.diff.changeValue(updatedPlaytime.subtract(previousPlaytime));
    gameLog.eventtype.changeValue("Played");
    gameLog.eventdate.changeValue(new Timestamp(new Date().getTime()));

    gameLog.commit(connection);
  }

  protected static String getFullUrl(String search) throws UnsupportedEncodingException {
    String encoded = URLEncoder.encode(search, "UTF-8");
    String api_key = System.getenv("giantbomb_api");
    return "http://www.giantbomb.com/api/search/" +
        "?api_key=" + api_key +
        "&format=json" +
        "&query=\"" + encoded + "\"" +
        "&resources=game" +
        "&field_list=id,name,image,original_release_date";
  }

  protected static JSONObject getSingleGameWithId(Integer id) throws IOException {
    String idUrl = getIdUrl(id);
    JSONObject jsonObject = readJsonFromUrl(idUrl);
    return jsonObject.getJSONObject("results");
  }

  protected static String getIdUrl(Integer id) throws UnsupportedEncodingException {
    String api_key = System.getenv("giantbomb_api");
    return "http://www.giantbomb.com/api/game/3030-" + id + "/" +
        "?api_key=" + api_key +
        "&format=json" +
        "&resources=game" +
        "&field_list=name,image,original_release_date";
  }


  public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
    try (InputStream is = new URL(url).openStream()) {
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      String jsonText = readAll(rd);
      return new JSONObject(jsonText);
    }
  }

  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }


  protected static void debug(Object object) {
    System.out.println(object);
  }


}
