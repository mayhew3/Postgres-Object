package com.mayhew3.mediamogul.archive;

import com.mayhew3.mediamogul.model.tv.ConnectLog;

public class ConnectLogFactory extends ArchiveableFactory<ConnectLog> {
  @Override
  public ConnectLog createEntity() {
    return new ConnectLog();
  }

  @Override
  public Integer monthsToKeep() {
    return 41;
  }

  @Override
  public String tableName() {
    return "connect_log";
  }

  @Override
  public String dateColumnName() {
    return "start_time";
  }
}
