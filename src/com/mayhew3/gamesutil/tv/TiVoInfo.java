package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.xml.NodeReader;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.NodeList;

public class TiVoInfo {
  String programId;
  String tivoId;
  public String seriesTitle;
  Boolean isSuggestion;
  public String url;
  Boolean recordingNow = false;

  public TiVoInfo(@NotNull NodeList showDetails, @NotNull NodeReader nodeReader) {
    programId = nodeReader.getValueOfSimpleStringNullableNode(showDetails, "ProgramId");
    tivoId = nodeReader.getValueOfSimpleStringNullableNode(showDetails, "SeriesId");
    seriesTitle = nodeReader.getValueOfSimpleStringNullableNode(showDetails, "Title");

    if (programId == null) {
      throw new RuntimeException("Episode found on TiVo with no ProgramId field!");
    }

    if (tivoId == null) {
      throw new RuntimeException("Episode found on TiVo with no SeriesId field!");
    }
  }

}
