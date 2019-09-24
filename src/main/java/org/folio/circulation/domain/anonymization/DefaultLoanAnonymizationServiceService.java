package org.folio.circulation.domain.anonymization;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.folio.circulation.domain.AnonymizeStorageLoansRepository;
import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.anonymization.checks.AnonymizationChecker;
import org.folio.circulation.support.Result;

/**
 * Validates loan eligibility for anonymization. By default a loan can only be anonymized if it's closed and there are no open fees
 * and fines associated with it.
 *
 */
public class DefaultLoanAnonymizationServiceService implements LoanAnonymizationService {

  private static final String CAN_BE_ANONYMIZED_KEY = "_";

  protected final LoanAnonymizationHelper anonymization;
  private final AnonymizeStorageLoansRepository anonymizeStorageLoansRepository;
  private final List<AnonymizationChecker> anonymizationCheckers;

    public DefaultLoanAnonymizationServiceService(LoanAnonymizationHelper anonymization) {
    this(anonymization, new ArrayList<>());
  }

  DefaultLoanAnonymizationServiceService(LoanAnonymizationHelper anonymization, List<AnonymizationChecker> checkers) {
    this.anonymization = anonymization;
    anonymizationCheckers = checkers;
    anonymizeStorageLoansRepository = new AnonymizeStorageLoansRepository(anonymization.clients());
  }

  @Override
  public CompletableFuture<Result<LoanAnonymizationRecords>> anonymizeLoans() {

    return anonymization.loansFinder().findLoansToAnonymize()
      .thenApply(r -> r.map(new LoanAnonymizationRecords()::withLoansFound))
      .thenCompose(this::segregateLoans)
      .thenCompose(r -> r.after(anonymizeStorageLoansRepository::postAnonymizeStorageLoans));
  }


  private CompletableFuture<Result<LoanAnonymizationRecords>> segregateLoans(
      Result<LoanAnonymizationRecords> anonymizationRecords) {

    return completedFuture(anonymizationRecords.map(records -> {
      HashSetValuedHashMap<String, String> multiMap = new HashSetValuedHashMap<>();
      for (Loan loan : records.getLoansFound()) {
        for (AnonymizationChecker checker : anonymizationCheckers) {
          if (!checker.canBeAnonymized(loan)) {
            multiMap.put(checker.getReason(), loan.getId());
          } else {
            multiMap.put(CAN_BE_ANONYMIZED_KEY, loan.getId());
          }
        }
      }

      return records.withAnonymizedLoans(multiMap.remove(CAN_BE_ANONYMIZED_KEY))
        .withNotAnonymizedLoans(multiMap);
    }));

  }
}
