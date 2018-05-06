package com.mayhew3.mediamogul.scheduler;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.mediamogul.tv.helper.UpdateMode;
import com.mayhew3.mediamogul.xml.BadlyFormattedXMLException;
import org.apache.http.auth.AuthenticationException;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.sql.SQLException;

public interface UpdateRunner {
  String getRunnerName();

  @Nullable
  UpdateMode getUpdateMode();

  default String getUniqueIdentifier() {
    UpdateMode updateMode = getUpdateMode();
    if (updateMode == null) {
      return getRunnerName();
    } else {
      return getRunnerName() + " (" + updateMode.getTypekey() + ")";
    }
  }

  void runUpdate() throws SQLException, BadlyFormattedXMLException, AuthenticationException, UnirestException, InterruptedException, FileNotFoundException;
}
