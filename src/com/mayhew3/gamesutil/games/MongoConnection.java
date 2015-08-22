package com.mayhew3.gamesutil.games;

import com.mongodb.*;

import java.net.UnknownHostException;

public class MongoConnection {

  private MongoClient _mongoClient;
  private DB _db;

  public MongoConnection(String dbName) {
    try {
      _mongoClient = new MongoClient("localhost");
      _db = _mongoClient.getDB(dbName);
    } catch (UnknownHostException e) {
      e.printStackTrace();
      throw new RuntimeException("Mongo connection failed.");
    }
  }

  public DB getDB() {
    return _db;
  }

  public DBCollection getCollection(String collectionName) {
    return _db.getCollection(collectionName);
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
}
