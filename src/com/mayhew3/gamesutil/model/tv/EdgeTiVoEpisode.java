package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.DataObject;
import com.mayhew3.gamesutil.dataobject.FieldValueInteger;
import com.mayhew3.gamesutil.dataobject.Nullability;

public class EdgeTiVoEpisode extends DataObject {

  public FieldValueInteger episodeId = registerIntegerField("episode_id", Nullability.NOT_NULL);
  public FieldValueInteger tivoEpisodeId = registerIntegerField("tivo_episode_id", Nullability.NOT_NULL);

  @Override
  protected String getTableName() {
    return "edge_tivo_episode";
  }

  @Override
  public String toString() {
    return id.getValue() + ": " + tivoEpisodeId.getValue() + " " + episodeId.getValue();
  }
}
