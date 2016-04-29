package com.mayhew3.gamesutil.dataobject;

public class EdgeTiVoEpisode extends DataObject {

  public FieldValueInteger episodeId = registerIntegerField("episode_id");
  public FieldValueInteger tivoEpisodeId = registerIntegerField("tivo_episode_id");

  @Override
  protected String getTableName() {
    return "edge_tivo_episode";
  }

  @Override
  public String toString() {
    return id.getValue() + ": " + tivoEpisodeId.getValue() + " " + episodeId.getValue();
  }
}
