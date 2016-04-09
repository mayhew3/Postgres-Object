package com.mayhew3.gamesutil.tv.example;

import com.google.common.collect.Lists;
import com.mayhew3.gamesutil.dataobject.EpisodeMongo;
import com.mayhew3.gamesutil.dataobject.FieldValue;
import com.mayhew3.gamesutil.dataobject.SeriesMongo;
import com.mayhew3.gamesutil.tv.TVDatabaseUtility;
import com.mongodb.*;
import com.sun.istack.internal.NotNull;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindDuplicateEpisodesUpdater extends TVDatabaseUtility {

  /**
   * NOTE: This never really worked all the way through. Use it for code snippets, but instead of using this utility,
   * I ended up using the Mongo "aggregate" query to find all duplicates that weren't a MatchingStump. It was only one.
   * So I manually fixed it.
   *
   * @throws UnknownHostException
   */


  public FindDuplicateEpisodesUpdater() throws UnknownHostException {
    super("tv");

  }

  // declare mappings from old -> new column names
  // utility should copy the values over, and delete the old field.
  public static void main(String[] args) {
    try {
      FindDuplicateEpisodesUpdater updater = new FindDuplicateEpisodesUpdater();
      updater.updateRows();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

  }

  public void updateRows() {
//    updateSerieses();

    updateEpisodes();

    debug("Completed.");
  }

  private void updateEpisodes() {
    DBCollection episodes = _db.getCollection("episodes");

    List<DBObject> queryObjects = Lists.newArrayList();

    queryObjects.add(new BasicDBObject("$group", new BasicDBObject("_id", "$TiVoProgramId")
        .append("count", new BasicDBObject("$sum", 1))));
    queryObjects.add(new BasicDBObject("$match", new BasicDBObject("_id", new BasicDBObject("$ne", null)).append("count", new BasicDBObject("$gt", 1))));
    queryObjects.add(new BasicDBObject("$project", new BasicDBObject("TiVoProgramId", "$_id").append("_id", 0)));
    AggregationOutput duplicates = episodes.aggregate(queryObjects);

    Iterable<DBObject> results = duplicates.results();

    Integer i = 0;
    for (DBObject resultObj : results) {
      String tiVoProgramId = (String) resultObj.get("TiVoProgramId");
      i++;

      debug("Processing + " + i + ": " + tiVoProgramId);

      resolveEpisodes(tiVoProgramId);
    }
  }

  private void resolveEpisodes(String tivoProgramId) {
    DBCursor cursor = _db.getCollection("episodes").find(new BasicDBObject("TiVoProgramId", tivoProgramId));

    List<EpisodeMongo> duplicates = new ArrayList<>();

    while (cursor.hasNext()) {
      DBObject next = cursor.next();
      EpisodeMongo episodeMongo = new EpisodeMongo();
      episodeMongo.initializeFromDBObject(next);

      duplicates.add(episodeMongo);
    }

    EpisodeMongo episodeToKeep = pickEpisodeToKeep(duplicates);

    deleteExtraEpisodes(duplicates, episodeToKeep);
  }

  private void deleteExtraEpisodes(List<EpisodeMongo> duplicates, EpisodeMongo episodeToKeep) {

  }

  @NotNull
  private EpisodeMongo pickEpisodeToKeep(List<EpisodeMongo> duplicates) {

    return null;
  }

  private EpisodeMongo getEpisodeWithFewestBlankTiVoFields(List<EpisodeMongo> duplicates) {
    Map<EpisodeMongo, Integer> blankCounts = new HashMap<>();

    for (EpisodeMongo duplicate : duplicates) {
      Integer blankCount = getTiVoBlankCount(duplicate);
    }
    return blankCounts.keySet().iterator().next();
  }

  private Integer getTiVoBlankCount(EpisodeMongo episodeMongo) {
    List<FieldValue> fieldsToCheck = Lists.newArrayList();
    fieldsToCheck.add(episodeMongo.tivoCaptureDate);
    fieldsToCheck.add(episodeMongo.tivoChannel);
    fieldsToCheck.add(episodeMongo.tivoDescription);
    fieldsToCheck.add(episodeMongo.tivoEpisodeNumber);
    fieldsToCheck.add(episodeMongo.tivoEpisodeTitle);
    fieldsToCheck.add(episodeMongo.tivoHD);
    fieldsToCheck.add(episodeMongo.tivoRating);
    fieldsToCheck.add(episodeMongo.tivoSeriesId);
    fieldsToCheck.add(episodeMongo.tivoSeriesTitle);
    fieldsToCheck.add(episodeMongo.tivoShowingDuration);
    fieldsToCheck.add(episodeMongo.tivoShowingStartTime);
    fieldsToCheck.add(episodeMongo.tivoStation);
    fieldsToCheck.add(episodeMongo.tivoSuggestion);
    fieldsToCheck.add(episodeMongo.tivoUrl);

    Integer nullFields = 0;

    for (FieldValue fieldValue : fieldsToCheck) {
      if (fieldValue.getValue() == null) {
        nullFields++;
      }
    }
    return nullFields;
  }

  private Integer getTVDBBlankCount(EpisodeMongo episodeMongo) {
    List<FieldValue> fieldsToCheck = Lists.newArrayList();
    fieldsToCheck.add(episodeMongo.tvdbAbsoluteNumber);
    fieldsToCheck.add(episodeMongo.tvdbAirsAfterSeason);
    fieldsToCheck.add(episodeMongo.tvdbAirsBeforeEpisode);
    fieldsToCheck.add(episodeMongo.tvdbAirsBeforeSeason);
    fieldsToCheck.add(episodeMongo.tvdbDirector);
    fieldsToCheck.add(episodeMongo.tvdbEpisodeId);
    fieldsToCheck.add(episodeMongo.tvdbEpisodeName);
    fieldsToCheck.add(episodeMongo.tvdbEpisodeNumber);
    fieldsToCheck.add(episodeMongo.tvdbFilename);
    fieldsToCheck.add(episodeMongo.tvdbFirstAired);
    fieldsToCheck.add(episodeMongo.tvdbLastUpdated);
    fieldsToCheck.add(episodeMongo.tvdbOverview);
    fieldsToCheck.add(episodeMongo.tvdbProductionCode);
    fieldsToCheck.add(episodeMongo.tvdbRating);
    fieldsToCheck.add(episodeMongo.tvdbRatingCount);
    fieldsToCheck.add(episodeMongo.tvdbSeason);
    fieldsToCheck.add(episodeMongo.tvdbSeriesName);

    Integer nullFields = 0;

    for (FieldValue fieldValue : fieldsToCheck) {
      if (fieldValue.getValue() == null) {
        nullFields++;
      }
    }
    return nullFields;
  }


  private void updateSerieses() {
    DBCollection collection = _db.getCollection("series");
    DBCursor cursor = collection.find();

    Integer totalSeries = cursor.count();
    Integer i = 0;

    while (cursor.hasNext()) {
      DBObject seriesObj = cursor.next();
      i++;

      debug("Processing series " + i + " of " + totalSeries);

      SeriesMongo seriesMongo = new SeriesMongo();
      seriesMongo.initializeFromDBObject(seriesObj);

      String tvdbSeriesId = seriesMongo.tvdbSeriesId.getValue();

      if (tvdbSeriesId != null) {
        DBCursor interiorCursor = collection.find(new BasicDBObject()
                .append("tvdbSeriesId", tvdbSeriesId)
                .append("_id", new BasicDBObject("$ne", seriesMongo._id.getValue()))
        );
        if (interiorCursor.hasNext()) {
          debug("Found duplicate for episode: " + seriesMongo + ", TVDB: " + tvdbSeriesId);
          seriesMongo.hasDuplicates.changeValue(true);
          seriesMongo.commit(_db);
        }
      }
    }
  }


}
