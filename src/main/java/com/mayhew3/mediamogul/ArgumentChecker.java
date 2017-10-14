package com.mayhew3.mediamogul;

import org.apache.commons.cli.*;

import java.util.Optional;

public class ArgumentChecker {
  private final CommandLine commands;

  public ArgumentChecker(String... args) {
    Options options = new Options();

    Option dbOption = Option.builder("db")
        .hasArg()
        .desc("Database")
        .required(true)
        .build();
    options.addOption(dbOption);

    Option modeOption = Option.builder("mode")
        .hasArg()
        .desc("Update Mode")
        .required(false)
        .build();
    options.addOption(modeOption);

    CommandLineParser parser = new DefaultParser();

    try {
      this.commands = parser.parse(options, args);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public Optional<String> getUpdateModeIdentifier() {
    if (commands.hasOption("mode")) {
      return Optional.of(commands.getOptionValue("mode"));
    } else {
      return Optional.empty();
    }
  }

  public String getDBIdentifier() {
    if (commands.hasOption("db")) {
      return commands.getOptionValue("db");
    } else {
      throw new IllegalStateException("No command line argument specified for 'db'!");
    }
  }
}
