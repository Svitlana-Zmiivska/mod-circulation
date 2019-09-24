package org.folio.circulation.domain.anonymization.checks;

import org.folio.circulation.domain.Account;
import org.folio.circulation.domain.Loan;

public class IntervalAfterFeesAndFinesCloseChecker implements AnonymizationChecker {


  private long secondsAfterLoanCloses;


  public IntervalAfterFeesAndFinesCloseChecker(long secondsAfterLoanCloses) {
    this.secondsAfterLoanCloses = secondsAfterLoanCloses;
  }

  @Override
  public boolean canBeAnonymized(Loan loan) {
    return loan.getAccounts().stream().allMatch(Account::isClosed);

  }

  @Override
  public String getReason() {
    return "intervalAfterFeesAndFinesClose";
  }
}