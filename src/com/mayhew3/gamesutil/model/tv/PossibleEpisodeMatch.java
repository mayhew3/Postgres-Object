package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.FieldValueBigDecimal;
import com.mayhew3.gamesutil.dataobject.FieldValueForeignKey;
import com.mayhew3.gamesutil.dataobject.FieldValueString;
import com.mayhew3.gamesutil.dataobject.Nullability;

public class PossibleEpisodeMatch extends RetireableDataObject {

  /* FK */
  public FieldValueForeignKey seriesId = registerForeignKey(new Series(), Nullability.NOT_NULL);
  public FieldValueForeignKey tivoEpisodeId = registerForeignKey(new TiVoEpisode(), Nullability.NOT_NULL);
  public FieldValueForeignKey tvdbEpisodeId = registerForeignKey(new TVDBEpisode(), Nullability.NOT_NULL);

  /* Data */
  public FieldValueBigDecimal matchScore = registerBigDecimalField("match_score", Nullability.NOT_NULL);
  public FieldValueString matchAlgorithm = registerStringField("match_algorithm", Nullability.NOT_NULL);

  public PossibleEpisodeMatch() {
    addUniqueConstraint(tivoEpisodeId, tvdbEpisodeId);
  }

  @Override
  public String getTableName() {
    return "possible_episode_match";
  }
}
