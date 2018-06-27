package org.folio.circulation.domain.validation;

import org.folio.circulation.domain.LoanAndRelatedRecords;
import org.folio.circulation.domain.RequestQueue;
import org.folio.circulation.domain.User;
import org.folio.circulation.support.HttpResult;
import org.folio.circulation.support.ValidationErrorFailure;

import java.util.function.Function;

import static org.folio.circulation.support.HttpResult.failed;

public class AwaitingPickupValidator {
  private final Function<String, ValidationErrorFailure> awaitingPickupErrorFunction;

  public AwaitingPickupValidator(
    Function<String, ValidationErrorFailure> stringValidationErrorFailureFunction) {

    awaitingPickupErrorFunction = stringValidationErrorFailureFunction;
  }

  public HttpResult<LoanAndRelatedRecords> refuseWhenUserIsNotAwaitingPickup(
    HttpResult<LoanAndRelatedRecords> loanAndRelatedRecords) {

    return loanAndRelatedRecords.next(loan -> {
      final RequestQueue requestQueue = loan.getRequestQueue();
      final User requestingUser = loan.getLoan().getUser();
      String itemTitle = loan.getLoan().getItem().getTitle();
      String itemBarcode = loan.getLoan().getItem().getBarcode();

        if(requestQueue.hasAwaitingPickupRequestForOtherPatron(requestingUser)) {
        return failed(awaitingPickupErrorFunction.apply(
            String.format("%s (Barcode: %s) cannot be checked out to user %s because it is awaiting pickup by another patron",
              itemTitle, itemBarcode, requestingUser.getPersonalName())));
      }
      else {
        return loanAndRelatedRecords;
      }
    });
  }
}
