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
import java.util.Date;

public class DatabaseUtility {

  protected static MongoClient _mongoClient;
  protected static DB _db;
  protected static DBCollection _collection;

  protected static void connect(String dbName) throws UnknownHostException {
    _mongoClient = new MongoClient("localhost");
    _db = _mongoClient.getDB(dbName);
    _collection = _db.getCollection(dbName);
  }

  protected static void debug(Object object) {
    System.out.println(object);
  }

  protected static String getFullUrl() {
    String steamKey = "5060C25283EB078B3CD231A9CDCE958F";
    return "http://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/" +
        "?key=" + steamKey +
        "&steamid=76561197970763625" +
        "&format=json" +
        "&include_appinfo=1";
  }

  protected static void addGame(Integer steamID, String gameName) {
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


  public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
    InputStream is = new URL(url).openStream();
    try {
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      String jsonText = readAll(rd);
      JSONObject json = new JSONObject(jsonText);
      return json;
    } finally {
      is.close();
    }
  }

  public static Document readXMLFromUrl(String urlString) throws IOException {
//    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//    keyStore.load("mySrvKeystore", "butthead");
//    trustStore.close();
//
//    TrustManagerFactory tmf =
//        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//    tmf.init(keyStore);
//    SSLContext ctx = SSLContext.getInstance("TLS");
//    ctx.init(null, tmf.getTrustManagers(), null);
//    SSLSocketFactory sslFactory = ctx.getSocketFactory();

    Authenticator.setDefault (new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("tivo", "4649000153".toCharArray());
      }
    });

    URL url = new URL(urlString);
//
//    String credentials = "tivo" + ":" + "4649000153";
//    String encoding = new BASE64Encoder().encode(credentials.getBytes("UTF-8"));
//    URLConnection uc = url.openConnection();
//    uc.setRequestProperty("Authorization", String.format("Basic %s", encoding));

    HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
//    conn.setSSLSocketFactory(sslFactory);
//    conn.setRequestMethod("POST");

//    HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
    InputStream is = conn.getInputStream();
    try {
      return recoverDocument(is);
    } finally {
      is.close();
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


  private static Document recoverDocument(InputStream inputStream) {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = null;
    try {
      dBuilder = dbFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }

    Document doc= null;
    try {
      assert dBuilder != null;
      doc = dBuilder.parse(inputStream);
    } catch (SAXException | IOException e) {
      e.printStackTrace();
    }
    return doc;
  }



  protected static void closeDatabase() {
    _mongoClient.close();
  }

  protected static DBCursor findSingleMatch(DBCollection episodes, String fieldName, String programId) {
    BasicDBObject query = new BasicDBObject(fieldName, programId);

    DBCursor cursor = episodes.find(query);

    if (cursor.count() == 1) {
      return cursor;
    } else if (cursor.count() == 0) {
      return null;
    } else {
      throw new IllegalStateException("Multiple matches found with " + fieldName + " field with value '" + programId + "'");
    }
  }
}
