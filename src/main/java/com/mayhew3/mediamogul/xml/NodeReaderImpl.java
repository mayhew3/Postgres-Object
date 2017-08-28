package com.mayhew3.mediamogul.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NodeReaderImpl implements NodeReader {
  private String localFilePath = null;

  public NodeReaderImpl() {
    // nothing for base constructor
  }

  public NodeReaderImpl(String localFilePath) {
    this.localFilePath = localFilePath;
  }

  @Override
  @NotNull
  public Document readXMLFromUrl(String urlString) throws IOException, SAXException {
    URL url = new URL(urlString);

    if (localFilePath != null) {
      File destination = new File(localFilePath);
      FileUtils.copyURLToFile(url, destination);
    }

    InputStream is = url.openStream();
    return recoverDocument(is);
  }

  @NotNull
  @Override
  public Document readXMLFromFile(String filePath) throws IOException, SAXException {
    File source = new File(filePath);
    FileInputStream fileInputStream = new FileInputStream(source);
    return recoverDocument(fileInputStream);
  }

  @NotNull
  @Override
  public Node getNodeWithTag(NodeList nodeList, String tag) throws BadlyFormattedXMLException {
    for (int x = 0; x < nodeList.getLength(); x++) {
      Node item = nodeList.item(x);
      if (tag.equalsIgnoreCase(item.getNodeName())) {
        return item;
      }
    }
    throw new BadlyFormattedXMLException("No node found with tag '" + tag + "'");
  }

  @Override
  public Node getNullableNodeWithTag(NodeList nodeList, String tag) {
    for (int x = 0; x < nodeList.getLength(); x++) {
      Node item = nodeList.item(x);
      if (tag.equalsIgnoreCase(item.getNodeName())) {
        return item;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public List<Node> getAllNodesWithTag(NodeList nodeList, String tag) {
    List<Node> matchingNodes = new ArrayList<>();
    for (int x = 0; x < nodeList.getLength(); x++) {
      Node item = nodeList.item(x);
      if (tag.equals(item.getNodeName())) {
        matchingNodes.add(item);
      }
    }
    return matchingNodes;
  }

  @Nullable
  @Override
  public String getValueOfSimpleStringNullableNode(NodeList nodeList, String tag) {
    Node nodeWithTag = getNullableNodeWithTag(nodeList, tag);
    return nodeWithTag == null ? null : parseSimpleStringFromNode(nodeWithTag);
  }

  @Override
  public @NotNull String getValueOfSimpleStringNode(NodeList nodeList, String tag) throws BadlyFormattedXMLException {
    @NotNull Node nodeWithTag = getNodeWithTag(nodeList, tag);

    @Nullable String nodeValue = parseSimpleStringFromNode(nodeWithTag);
    if (nodeValue == null) {
      throw new BadlyFormattedXMLException("No text children found for node '" + nodeWithTag.getNodeName() + "'");
    }
    return nodeValue;
  }


  @Nullable
  private String parseSimpleStringFromNode(Node nodeWithTag) {
    NodeList childNodes = nodeWithTag.getChildNodes();
    if (childNodes.getLength() > 1) {
      throw new RuntimeException("Expect only one text child of node '" + nodeWithTag.getNodeName() + "'");
    } else if (childNodes.getLength() == 0) {
      return null;
    }
    Node textNode = childNodes.item(0);
    return textNode.getNodeValue();
  }

  @NotNull
  public Document recoverDocument(InputStream inputStream) throws SAXException, IOException {
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
