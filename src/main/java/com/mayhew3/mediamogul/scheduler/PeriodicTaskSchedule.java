package com.mayhew3.mediamogul.scheduler;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

public class PeriodicTaskSchedule extends TaskSchedule {
  private Integer minutesBetween;

  PeriodicTaskSchedule(UpdateRunner updateRunner, Integer minutesBetween) {
    super(updateRunner);
    this.minutesBetween = minutesBetween;
  }

  @NotNull
  @Override
  public Boolean isEligibleToRun() {
    if (lastRan == null) {
      return true;
    }
    Minutes minutes = Minutes.minutesBetween(new DateTime(lastRan), new DateTime());
    return minutes.getMinutes() > minutesBetween;
  }
}
