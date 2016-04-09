package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.xml.NodeReader;
import com.sun.istack.internal.NotNull;
import org.w3c.dom.NodeList;

public class TiVoInfo {
  public String programId;
  public String tivoId;
  public String seriesTitle;
  public Boolean isSuggestion;
  public String url;

  public TiVoInfo(@NotNull NodeList showDetails, @NotNull NodeReader nodeReader) {
    programId = nodeReader.getValueOfSimpleStringNode(showDetails, "ProgramId");
    tivoId = nodeReader.getValueOfSimpleStringNode(showDetails, "SeriesId");
    seriesTitle = nodeReader.getValueOfSimpleStringNode(showDetails, "Title");

    if (programId == null) {
      throw new RuntimeException("Episode found on TiVo with no ProgramId field!");
    }

    if (tivoId == null) {
      throw new RuntimeException("Episode found on TiVo with no SeriesId field!");
    }
  }

}
