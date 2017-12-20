package com.mayhew3.mediamogul.tv.blog;

import com.google.common.collect.Maps;

import java.util.Map;

public class BlogTemplatePrinter {

  private Map<String, String> tagMappings;
  private String inputTemplate;

  public BlogTemplatePrinter(String inputTemplate) {
    this.tagMappings = Maps.newHashMap();
    this.inputTemplate = inputTemplate;
  }

  public void addMapping(String tagName, String tagValue) {
    tagMappings.put(tagName, tagValue);
  }

  public void clearMappings() {
    tagMappings = Maps.newHashMap();
  }

  public String createCombinedExport() {
    String outputText = inputTemplate;

    outputText = outputText.replace("\n", "");
    outputText = outputText.replace("\r", "");
    outputText = outputText.replace("\t", "");
    outputText = outputText.replace("    ", "");


    for (String tagName : tagMappings.keySet()) {
      int foundLocation = inputTemplate.indexOf("$" + tagName);
      if (foundLocation > 0) {
        String tagValue = tagMappings.get(tagName);
        outputText = outputText.replace("$" + tagName, tagValue);
      }
    }

    return outputText;
  }

}
