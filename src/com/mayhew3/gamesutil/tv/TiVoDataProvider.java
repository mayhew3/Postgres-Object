package com.mayhew3.gamesutil.tv;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

public interface TiVoDataProvider {

  Document connectAndRetrieveDocument(String urlString) throws IOException, SAXException;
  RemoteFileDownloader withCopySavedTo(String localFilePath);
}
