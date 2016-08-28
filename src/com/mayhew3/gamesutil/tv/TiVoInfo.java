package com.mayhew3.gamesutil.tv;

import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import com.mayhew3.gamesutil.xml.NodeReader;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.NodeList;

public class TiVoInfo {
  String programId;
  String tivoId;
  public String seriesTitle;
  String captureDate;
  Boolean isSuggestion;
  public String url;
  Boolean recordingNow = false;

  public TiVoInfo(@NotNull NodeList showDetails, @NotNull NodeReader nodeReader) throws BadlyFormattedXMLException {
    programId = nodeReader.getValueOfSimpleStringNode(showDetails, "ProgramId");
    tivoId = nodeReader.getValueOfSimpleStringNode(showDetails, "SeriesId");
    seriesTitle = nodeReader.getValueOfSimpleStringNode(showDetails, "Title");
    captureDate = nodeReader.getValueOfSimpleStringNode(showDetails, "CaptureDate");
  }

}
