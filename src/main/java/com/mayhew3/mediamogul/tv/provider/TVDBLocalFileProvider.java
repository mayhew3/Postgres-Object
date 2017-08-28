package com.mayhew3.mediamogul.tv.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TVDBLocalFileProvider implements TVDBDataProvider {

  private String localFilePath;

  public TVDBLocalFileProvider(String localFilePath) {
    this.localFilePath = localFilePath;
  }

  @Override
  public InputStream getEpisodeData(Integer tvdbId) throws IOException {
    File source = new File(localFilePath);
    return new FileInputStream(source);
  }

  @Override
  public InputStream findSeriesMatches(String formattedTitle) throws IOException {
    File source = new File(localFilePath);
    return new FileInputStream(source);
  }
}
