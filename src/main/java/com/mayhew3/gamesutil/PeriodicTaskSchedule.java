package com.mayhew3.gamesutil;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.Date;

public class PeriodicTaskSchedule extends TaskSchedule {
  private Integer secondsBetween;

  PeriodicTaskSchedule(UpdateRunner updateRunner, Integer secondsBetween) {
    super(updateRunner);
    this.secondsBetween = secondsBetween;
  }

  @Override
  public Boolean isEligibleToRun() {
    Date lastRan = getLastRan();
    if (lastRan == null) {
      return true;
    }
    Seconds seconds = Seconds.secondsBetween(new DateTime(lastRan), new DateTime());
    return seconds.getSeconds() > secondsBetween;
  }
}
