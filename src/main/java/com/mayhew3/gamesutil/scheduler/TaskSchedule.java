package com.mayhew3.gamesutil.scheduler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public abstract class TaskSchedule {
  private UpdateRunner updateRunner;

  @Nullable
  Date lastRan = null;

  TaskSchedule(UpdateRunner updateRunner) {
    this.updateRunner = updateRunner;
  }

  @NotNull
  UpdateRunner getUpdateRunner() {
    return updateRunner;
  }

  void updateLastRanToNow() {
    this.lastRan = new Date();
  }

  @NotNull
  public abstract Boolean isEligibleToRun();
}
