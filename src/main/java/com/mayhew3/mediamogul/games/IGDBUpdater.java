package com.mayhew3.mediamogul.games;

import callback.OnSuccessCallback;
import com.google.common.collect.Lists;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mayhew3.mediamogul.ArgumentChecker;
import com.mayhew3.mediamogul.db.PostgresConnectionFactory;
import com.mayhew3.mediamogul.db.SQLConnection;
import com.mayhew3.mediamogul.scheduler.UpdateRunner;
import com.mayhew3.mediamogul.tv.helper.UpdateMode;
import com.mayhew3.mediamogul.xml.BadlyFormattedXMLException;
import org.apache.http.auth.AuthenticationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import wrapper.IGDBWrapper;
import wrapper.Parameters;
import wrapper.Version;

import java.io.*;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class IGDBUpdater implements UpdateRunner {

  private SQLConnection connection;
  private IGDBWrapper wrapper;

  public IGDBUpdater(SQLConnection connection) {
    this.connection = connection;
    this.wrapper = new IGDBWrapper(System.getenv("igdb_key"), Version.STANDARD, false);
  }

  public static void main(String[] args) throws SQLException, FileNotFoundException, URISyntaxException {
    List<String> argList = Lists.newArrayList(args);
    Boolean singleGame = argList.contains("SingleGame");
    Boolean logToFile = argList.contains("LogToFile");
    ArgumentChecker argumentChecker = new ArgumentChecker(args);

    if (logToFile) {
      String mediaMogulLogs = System.getenv("MediaMogulLogs");

      File file = new File(mediaMogulLogs + "\\IGDBUpdaterErrors.log");
      FileOutputStream fos = new FileOutputStream(file, true);
      PrintStream ps = new PrintStream(fos);
      System.setErr(ps);

      System.err.println("Starting run on " + new Date());
    }

    IGDBUpdater igdbUpdater = new IGDBUpdater(PostgresConnectionFactory.createConnection(argumentChecker));

    igdbUpdater.updateTest();

  }

  private void updateTest() {
    Parameters parameters = new Parameters()
        .addSearch("Forza Horizon 4")
        .addFields("name,cover")
        .addLimit("5")
        .addOffset("0");


    wrapper.games(parameters, new OnSuccessCallback() {
      @Override
      public void onSuccess(@NotNull JSONArray jsonArray) {
        JSONArray result = jsonArray;
      }

      @Override
      public void onError(@NotNull Exception e) {
        int i = 0;
      }
    });
  }


  protected static void debug(Object object) {
    System.out.println(object);
  }


  @Override
  public String getRunnerName() {
    return "IGDB Updater";
  }

  @Override
  public @Nullable UpdateMode getUpdateMode() {
    return null;
  }

  @Override
  public String getUniqueIdentifier() {
    return "igdb_updater";
  }

  @Override
  public void runUpdate() throws SQLException, BadlyFormattedXMLException, AuthenticationException, UnirestException, InterruptedException, IOException {

  }
}
