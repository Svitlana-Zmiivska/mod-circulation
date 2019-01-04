package org.folio.circulation.domain.policy;

import org.folio.circulation.support.HttpResult;
import org.folio.circulation.support.http.server.ValidationError;
import org.joda.time.DateTime;

import java.util.function.Function;

/**
 * Overrides {@link #calculateDueDate(DateTime, DateTime)} in {@link RollingRenewalDueDateStrategy}
 * to skip due date truncating
 */
class RollingRenewalOverrideDueDateStrategy extends RollingRenewalDueDateStrategy {

  RollingRenewalOverrideDueDateStrategy(String loanPolicyId, String loanPolicyName, DateTime systemDate,
                                        String renewFrom, Period period,
                                        FixedDueDateSchedules dueDateLimitSchedules,
                                        Function<String, ValidationError> errorForPolicy) {
    super(loanPolicyId, loanPolicyName, systemDate, renewFrom, period, dueDateLimitSchedules, errorForPolicy);
  }

  @Override
  protected HttpResult<DateTime> calculateDueDate(DateTime from, DateTime loanDate) {
    return super.renewalDueDate(from);
  }
}

