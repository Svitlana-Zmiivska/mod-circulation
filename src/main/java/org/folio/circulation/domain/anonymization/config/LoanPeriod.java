package org.folio.circulation.domain.anonymization.config;

/**
 * Entity for loan`s 'duration' and 'invtervalId' configurations.
 */

//TODO REPLACE
public class LoanPeriod {

  private int duration;
  private Interval intervalId;

  LoanPeriod(int duration, Interval intervalId) {
    this.duration = duration;
    this.intervalId = intervalId;
  }

  public int getDuration() {
    return duration;
  }

  public Interval getIntervalId() {
    return intervalId;
  }
}
