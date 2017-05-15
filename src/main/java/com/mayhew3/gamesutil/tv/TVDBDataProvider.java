package com.mayhew3.gamesutil.tv;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public interface TVDBDataProvider {
  InputStream getEpisodeData(Integer tvdbId) throws IOException, SAXException;

  InputStream findSeriesMatches(String formattedTitle) throws IOException, SAXException;
}
