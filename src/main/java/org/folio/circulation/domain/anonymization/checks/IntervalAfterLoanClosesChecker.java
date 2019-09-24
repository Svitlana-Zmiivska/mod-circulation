package org.folio.circulation.domain.anonymization.checks;

import org.folio.circulation.domain.Loan;
import org.joda.time.DateTime;
import org.joda.time.Interval;

public class IntervalAfterLoanClosesChecker implements AnonymizationChecker {

  private long secondsAfterLoanCloses;

  public IntervalAfterLoanClosesChecker(long secondsAfterLoanCloses) {
    this.secondsAfterLoanCloses = secondsAfterLoanCloses;
  }

  @Override
  public boolean canBeAnonymized(Loan loan) {

    return new Interval(loan.getReturnDate(), DateTime.now()).toDuration()
      .getStandardSeconds() > secondsAfterLoanCloses;

  }

  @Override
  public String getReason() {
    return "intervalAfterLoanCloses";
  }
}