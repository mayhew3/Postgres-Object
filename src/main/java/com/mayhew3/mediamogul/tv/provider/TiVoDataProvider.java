package com.mayhew3.mediamogul.tv.provider;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

public interface TiVoDataProvider {

  Document connectAndRetrieveDocument(String urlString, @Nullable String episodeIdentifier) throws IOException, SAXException;
}
