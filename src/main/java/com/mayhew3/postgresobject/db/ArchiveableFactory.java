package com.mayhew3.postgresobject.db;

import com.mayhew3.postgresobject.dataobject.DataObject;

public abstract class ArchiveableFactory<T extends DataObject> {
  public abstract T createEntity();
  public abstract Integer monthsToKeep();
  public abstract String tableName();
  public abstract String dateColumnName();
  public abstract String otherColumnName();
  public abstract Object otherColumnValue();
}
