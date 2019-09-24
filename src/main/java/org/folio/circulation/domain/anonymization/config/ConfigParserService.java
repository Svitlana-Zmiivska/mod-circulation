package org.folio.circulation.domain.anonymization.config;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.circulation.domain.anonymization.LoanAnonymizationRecords;
import org.folio.circulation.domain.anonymization.checks.AnonymizationChecker;
import org.folio.circulation.support.Result;

public interface ConfigParserService {

  List<AnonymizationChecker> getCheckersFromTenantConfiguration(LoanHistoryTenantConfiguration config);
}
