package com.mayhew3.gamesutil.tv;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

public interface TiVoDataProvider {

  Document connectAndRetrieveDocument(String urlString, @Nullable String episodeIdentifier) throws IOException, SAXException;
  void withCopySaved();
  void withNoCopySaved();
}
