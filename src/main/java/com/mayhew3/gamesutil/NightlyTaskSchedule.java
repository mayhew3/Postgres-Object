package com.mayhew3.gamesutil;

import org.joda.time.LocalTime;

public class NightlyTaskSchedule extends TaskSchedule {

  private LocalTime startTime;
  private LocalTime endTime;

  private Integer numberOfDays;

  protected NightlyTaskSchedule(UpdateRunner updateRunner, LocalTime startTime, LocalTime endTime, Integer numberOfDays) {
    super(updateRunner);
    this.startTime = startTime;
    this.endTime = endTime;
    this.numberOfDays = numberOfDays;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public Integer getNumberOfDays() {
    return numberOfDays;
  }

  @Override
  public Boolean isEligibleToRun() {
    throw new IllegalStateException("Implement me!");
  }
}
