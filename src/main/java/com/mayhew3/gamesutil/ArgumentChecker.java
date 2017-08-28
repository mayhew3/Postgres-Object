package com.mayhew3.gamesutil;

import com.google.common.collect.Lists;
import org.apache.commons.cli.*;

import java.util.List;
import java.util.Optional;

public class ArgumentChecker {
  private final CommandLine commands;
  private List<String> argList;

  public ArgumentChecker(String... args) {
    Options options = new Options();
    Option typeOption = Option.builder("type")
        .hasArg()
        .desc("TVDB Update Type")
        .required(false)
        .build();
    options.addOption(typeOption);
    CommandLineParser parser = new DefaultParser();

    try {
      this.commands = parser.parse(options, args);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    this.argList = Lists.newArrayList(args);
  }

  public Optional<String> getUpdateTypeIdentifier() {
    if (commands.hasOption("type")) {
      return Optional.of(commands.getOptionValue("type"));
    } else {
      return Optional.empty();
    }
  }

  public String getDBIdentifier() {
    if (argList.contains("Test")) {
      return "test";
    }
    if (argList.contains("Local")) {
      return "local";
    }
    if (argList.contains("Heroku")) {
      return "heroku";
    }
    if (argList.contains("Demo")) {
      return "demo";
    }
    throw new IllegalArgumentException("Need to specify either Test or Local or Heroku in arguments.");
  }
}
