package com.mayhew3.gamesutil;

import java.util.Date;

public abstract class TaskSchedule {
  private UpdateRunner updateRunner;
  private Date lastRan = null;

  protected TaskSchedule(UpdateRunner updateRunner) {
    this.updateRunner = updateRunner;
  }

  public UpdateRunner getUpdateRunner() {
    return updateRunner;
  }

  public Date getLastRan() {
    return lastRan;
  }

  public void updateLastRanToNow() {
    this.lastRan = new Date();
  }

  public abstract Boolean isEligibleToRun();
}
