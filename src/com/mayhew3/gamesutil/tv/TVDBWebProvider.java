package com.mayhew3.gamesutil.tv;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class TVDBWebProvider implements TVDBDataProvider {

  private String localFilePath = null;

  public TVDBWebProvider() {
    // nothing for base constructor;
  }

  public TVDBWebProvider(String localFilePath) {
    this.localFilePath = localFilePath;
  }

  @Override
  public InputStream getEpisodeData(Integer tvdbId) throws IOException {
    String apiKey = System.getenv("TVDB_API_KEY");
    if (apiKey == null) {
      throw new IllegalStateException("No TVDB_API_KEY environment variable found!");
    }
    String urlString = "http://thetvdb.com/api/" + apiKey + "/series/" + tvdbId + "/all/en.xml";

    URL url = new URL(urlString);

    if (localFilePath != null) {
      File destination = new File(localFilePath);
      FileUtils.copyURLToFile(url, destination);
    }

    return url.openStream();
  }

  @Override
  public InputStream findSeriesMatches(String formattedTitle) throws IOException {
    String tvdbUrl = "http://thetvdb.com/api/GetSeries.php?seriesname=" + formattedTitle;
    URL url = new URL(tvdbUrl);
    return url.openStream();
  }
}
