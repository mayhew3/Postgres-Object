package com.mayhew3.gamesutil.scheduler;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.gamesutil.xml.BadlyFormattedXMLException;
import org.apache.http.auth.AuthenticationException;

import java.sql.SQLException;

public interface UpdateRunner {
  String getRunnerName();

  void runUpdate() throws SQLException, BadlyFormattedXMLException, AuthenticationException, UnirestException, InterruptedException;
}
