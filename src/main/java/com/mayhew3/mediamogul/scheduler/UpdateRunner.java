package com.mayhew3.mediamogul.scheduler;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.mediamogul.xml.BadlyFormattedXMLException;
import org.apache.http.auth.AuthenticationException;

import java.sql.SQLException;

public interface UpdateRunner {
  String getRunnerName();

  void runUpdate() throws SQLException, BadlyFormattedXMLException, AuthenticationException, UnirestException, InterruptedException;
}
