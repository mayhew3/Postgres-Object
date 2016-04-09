package com.mayhew3.gamesutil.dataobject;

public class EdgeTiVoEpisodePostgres extends DataObject {

  public FieldValueInteger episodeId = registerIntegerField("episode_id");
  public FieldValueInteger tivoEpisodeId = registerIntegerField("tivo_episode_id");

  public FieldValueInteger retired = registerIntegerField("retired");

  @Override
  protected String getTableName() {
    return "edge_tivo_episode";
  }

  @Override
  public String toString() {
    return id.getValue() + ": " + tivoEpisodeId.getValue() + " " + episodeId.getValue();
  }
}
