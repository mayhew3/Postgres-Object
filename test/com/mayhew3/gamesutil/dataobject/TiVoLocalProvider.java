package com.mayhew3.gamesutil.dataobject;

import com.mayhew3.gamesutil.tv.RemoteFileDownloader;
import com.mayhew3.gamesutil.tv.TiVoDataProvider;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TiVoLocalProvider implements TiVoDataProvider {
  private String localFilePath;
  private String listFileName;
  private String detailFileName;

  public TiVoLocalProvider(String localFilePath, String listFileName, String detailFileName) {
    this.localFilePath = localFilePath;
    this.listFileName = listFileName;
    this.detailFileName = detailFileName;
  }

  @Override
  public Document connectAndRetrieveDocument(String urlString) throws IOException, SAXException {
    if (urlString.contains("TiVoVideoDetails")) {
      return parseDocumentWithFile(localFilePath + detailFileName);
    } else if (urlString.contains("QueryContainer")) {
      return parseDocumentWithFile(localFilePath + listFileName);
    } else {
      throw new RuntimeException("Unrecognized url type.");
    }
  }

  private Document parseDocumentWithFile(String fullFilePath) throws IOException, SAXException {
    FileInputStream fileInputStream = new FileInputStream(fullFilePath);
    return recoverDocument(fileInputStream);
  }

  @Override
  public RemoteFileDownloader withCopySavedTo(String localFilePath) {
    return null;
  }

  private Document recoverDocument(InputStream inputStream) throws IOException, SAXException {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = null;
    try {
      dBuilder = dbFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }

    Document doc;
    assert dBuilder != null;
    doc = dBuilder.parse(inputStream);
    return doc;
  }

}
