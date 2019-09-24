package org.folio.circulation.domain.anonymization;

import java.lang.invoke.MethodHandles;
import java.util.Collections;

import org.folio.circulation.domain.anonymization.checks.HasNoAssociatedFeesAndFines;
import org.folio.circulation.domain.anonymization.config.LoanHistoryTenantConfiguration;
import org.folio.circulation.support.Clients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoanAnonymizationHelper {

  static final int FETCH_LOANS_LIMIT = 5000;
  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup()
    .lookupClass());
  private final Clients clients;

  private LoanAnonymizationFinderService loansFinderService;

  public LoanAnonymizationHelper(Clients clients) {
    this.clients = clients;
  }

  public LoanAnonymizationService byUserId(String userId) {
    log.info("Initializing loan anonymization for borrower");

    loansFinderService = new LoansForBorrowerFinder(this, userId);
    return new DefaultLoanAnonymizationServiceService(this,
        Collections.singletonList(new HasNoAssociatedFeesAndFines()));
  }

  public LoanAnonymizationService byCurrentTenant(LoanHistoryTenantConfiguration config) {
    log.info("Initializing loan anonymization for current tenant");
    loansFinderService = new LoansForTenantFinder(this);
    return new DefaultLoanAnonymizationServiceService(this, config.getLoanAnonymizationCheckers());
  }

  Clients clients() {
    return clients;
  }

  LoanAnonymizationFinderService loansFinder() {
    return loansFinderService;
  }
}
