package com.mayhew3.gamesutil.tv;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RemoteFileDownloader implements TiVoDataProvider {
  @Nullable private String localFilePath;

  public RemoteFileDownloader() {
    String tivoApiKey = System.getenv("TIVO_API_KEY");
    if (tivoApiKey == null) {
      throw new IllegalStateException("No TIVO_API_KEY environment variable found!");
    }

    Authenticator.setDefault (new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("tivo", tivoApiKey.toCharArray());
      }
    });
  }

  public Document connectAndRetrieveDocument(String urlString) throws IOException, SAXException {
    URL url = new URL(urlString);

    URLConnection urlConnection = url.openConnection();
    urlConnection.setConnectTimeout(15000);
    urlConnection.setReadTimeout(30000);

    try (InputStream is = urlConnection.getInputStream()) {

      if (localFilePath != null) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");

        File destination = new File("resources\\tivo_xml_" + simpleDateFormat.format(new Date()));
        FileOutputStream fos = new FileOutputStream(destination, true);

        ReadableByteChannel readableByteChannel = Channels.newChannel(is);
        FileChannel destChannel = fos.getChannel();

        destChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        fos.close();

        FileInputStream fileInputStream = new FileInputStream(destination);
        return recoverDocument(fileInputStream);
      }

      return recoverDocument(is);
    }
  }

  public RemoteFileDownloader withCopySavedTo(String localFilePath) {
    this.localFilePath = localFilePath;
    return this;
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
