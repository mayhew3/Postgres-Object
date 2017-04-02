package com.mayhew3.gamesutil.model.tv;

import com.mayhew3.gamesutil.dataobject.FieldValueBigDecimal;
import com.mayhew3.gamesutil.dataobject.FieldValueForeignKey;
import com.mayhew3.gamesutil.dataobject.FieldValueString;
import com.mayhew3.gamesutil.dataobject.Nullability;
import org.jetbrains.annotations.NotNull;

public class PossibleEpisodeMatch extends RetireableDataObject implements Comparable<PossibleEpisodeMatch> {

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

  @Override
  public int compareTo(@NotNull PossibleEpisodeMatch otherMatch) {
    return matchScore.getValue().compareTo(otherMatch.matchScore.getValue());
  }
}
