package com.mayhew3.gamesutil;

import com.mongodb.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

public class TVDatabaseUtility {

  protected MongoClient _mongoClient;
  protected DB _db;

  public TVDatabaseUtility(MongoClient client, DB db) {
    _mongoClient = client;
    _db = db;
  }

  public TVDatabaseUtility(String dbName) throws UnknownHostException {
    _mongoClient = new MongoClient("localhost");
    _db = _mongoClient.getDB(dbName);
  }

  protected void debug(Object object) {
    System.out.println(object);
  }

  protected String getFullUrl() {
    String steamKey = "5060C25283EB078B3CD231A9CDCE958F";
    return "http://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/" +
        "?key=" + steamKey +
        "&steamid=76561197970763625" +
        "&format=json" +
        "&include_appinfo=1";
  }
/*

  protected void addGame(Integer steamID, String gameName) {
    try {
      BasicDBObject gameObject = new BasicDBObject("Game", gameName)
          .append("SteamID", steamID)
          .append("Platform", "Steam")
          .append("Owned", true)
          .append("Started", false)
          .append("Added", new Date());
      _collection.insert(gameObject);
    } catch (MongoException e) {
      debug("Error inserting game '" + gameName + "' (" + steamID + "). Exception: ");
      e.printStackTrace();
    }
  }
*/


  public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
    try (InputStream is = new URL(url).openStream()) {
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      String jsonText = readAll(rd);
      return new JSONObject(jsonText);
    }
  }

  public Document readXMLFromTivoUrl(String urlString) throws IOException, SAXException {

    Authenticator.setDefault (new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("tivo", "4649000153".toCharArray());
      }
    });

    URL url = new URL(urlString);

    HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
    try (InputStream is = conn.getInputStream()) {
      return recoverDocument(is);
    }
  }

  public Document readXMLFromUrl(String urlString) throws IOException, SAXException {
      InputStream is = new URL(urlString).openStream();
      return recoverDocument(is);
  }

  private String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }


  protected Document recoverDocument(InputStream inputStream) throws IOException, SAXException {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = null;
    try {
      dBuilder = dbFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }

    Document doc;
    assert dBuilder != null;
    doc = dBuilder.parse(inputStream);
    return doc;
  }



  protected void closeDatabase() {
    _mongoClient.close();
  }

  protected DBObject findSingleMatch(DBCollection collection, String key, Object value) {
    BasicDBObject query = new BasicDBObject(key, value);

    DBCursor cursor = collection.find(query);

    if (cursor.count() == 1) {
      return cursor.next();
    } else if (cursor.count() == 0) {
      return null;
    } else {
      throw new IllegalStateException("Multiple matches found with " + key + " field with value '" + value + "'");
    }
  }

  protected DBObject findSingleMatch(String collectionName, String key, Object value) {
    if (!_db.collectionExists(collectionName)) {
      throw new IllegalStateException("No collection '" + collectionName + "' found.");
    }

    DBCollection collection = _db.getCollection(collectionName);

    return findSingleMatch(collection, key, value);
  }

  protected void singleFieldUpdateWithId(String collectionName, Object id, String fieldName, Object value) {
    BasicDBObject updateQuery = new BasicDBObject(fieldName, value);

    updateObjectWithId(collectionName, id, updateQuery);
  }

  protected void updateObjectWithId(String collectionName, Object id, DBObject updateQuery) {
    BasicDBObject queryObject = new BasicDBObject("_id", id);

    updateCollectionWithQuery(collectionName, queryObject, updateQuery);
  }

  protected WriteResult updateCollectionWithQuery(String collectionName, BasicDBObject queryObject, DBObject updateObject) {
    return _db.getCollection(collectionName).update(queryObject, new BasicDBObject("$set", updateObject));
  }

  protected WriteResult updateCollectionWithQueryMultiple(String collectionName, BasicDBObject queryObject, DBObject updateObject) {
    return _db.getCollection(collectionName).update(queryObject, new BasicDBObject("$set", updateObject), false, true);
  }
}
