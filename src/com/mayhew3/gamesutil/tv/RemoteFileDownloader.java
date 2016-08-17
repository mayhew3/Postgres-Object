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
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RemoteFileDownloader {
  private String urlString;
  @Nullable private String localFilePath;

  public RemoteFileDownloader(String url) {
    this.urlString = url;
  }

  public RemoteFileDownloader withAuthentication(String username, String password) {
    Authenticator.setDefault (new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password.toCharArray());
      }
    });
    return this;
  }

  public Document connectAndRetrieveDocument() throws IOException, SAXException {
    URL url = new URL(urlString);

    try (InputStream is = url.openStream()) {

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
