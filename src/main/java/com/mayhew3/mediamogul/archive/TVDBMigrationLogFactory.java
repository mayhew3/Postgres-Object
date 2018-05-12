package com.mayhew3.mediamogul.archive;

import com.mayhew3.mediamogul.model.tv.TVDBMigrationLog;

public class TVDBMigrationLogFactory extends ArchiveableFactory<TVDBMigrationLog> {
  @Override
  public TVDBMigrationLog createEntity() {
    return new TVDBMigrationLog();
  }

  @Override
  public Integer monthsToKeep() {
    return 6;
  }

  @Override
  public String tableName() {
    return "tvdb_migration_log";
  }

  @Override
  public String dateColumnName() {
    return "date_added";
  }
}
