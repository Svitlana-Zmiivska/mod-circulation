package api.loans.anonymization;

import static api.support.matchers.LoanMatchers.isAnonymized;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import java.net.MalformedURLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import api.support.builders.CheckOutByBarcodeRequestBuilder;
import api.support.builders.LoanHistoryConfigurationBuilder;
import org.folio.circulation.support.http.client.IndividualResource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Test;

public class AnonymizeLoansImmediatelyAPITests extends LoanAnonymizationTests {

  /**
   *     Given:
   *         An Anonymize closed loans setting of "Immediately after loan closes"
   *         An Anonymize closed loans with associated fees/fines setting of "Immediately after fee/fine closes"
   *         An open loan with an associated fee/fine
   *     When the item in the loan is checked in
   *     Then do not anonymize the loan
   */
  @Test
  public void shouldNotAnonymizeClosedLoansWithOpenFeesAndFinesAndSettingsOfAnonymizeImmediately()
    throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {

    LoanHistoryConfigurationBuilder loanHistoryConfig = new LoanHistoryConfigurationBuilder()
      .loanCloseAnonymizeImmediately()
      .feeFineCloseAnonymizeImmediately();
    createConfiguration(loanHistoryConfig);

    IndividualResource loanResource = loansFixture.checkOutByBarcode(new CheckOutByBarcodeRequestBuilder().forItem(item1)
      .to(user)
      .at(servicePoint.getId()));
    UUID loanID = loanResource.getId();

    createOpenAccountWithFeeFines(loanResource);
    loansFixture.checkInByBarcode(item1);

    anonymizeLoansInTenant();

    assertThat(loansStorageClient.getById(loanID)
      .getJson(), not(isAnonymized()));
  }

  /**
   *     Given:
   *         An Anonymize closed loans setting of "Immediately after loan closes"
   *         An Anonymize closed loans with associated fees/fines setting of "Immediately after fee/fine closes"
   *         A closed loan with an associated fee/fine
   *     When all fees/fines associated with the loan are closed
   *     Then anonymize the loan
   */
  @Test
  public void shouldAnonymizeClosedLoansWhenFeesAndFinesCloseAndSettingsOfAnonymizeImmediately()
    throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {

    LoanHistoryConfigurationBuilder loanHistoryConfig = new LoanHistoryConfigurationBuilder()
      .loanCloseAnonymizeImmediately()
      .feeFineCloseAnonymizeImmediately();
    createConfiguration(loanHistoryConfig);

    IndividualResource loanResource = loansFixture.checkOutByBarcode(new CheckOutByBarcodeRequestBuilder().forItem(item1)
      .to(user)
      .at(servicePoint.getId()));
    UUID loanID = loanResource.getId();

    createClosedAccountWithFeeFines(loanResource, DateTime.now());

    loansFixture.checkInByBarcode(item1);

    anonymizeLoansInTenant();

    assertThat(loansStorageClient.getById(loanID)
      .getJson(), isAnonymized());
  }

  /**
   *     Given:
   *         An Anonymize closed loans setting of "Immediately after loan closes"
   *         An Anonymize closed loans with associated fees/fines setting of "Never"
   *         An open loan with an associated fee/fine
   *     When the item in the loan is checked in
   *     Then do not anonymize the loan
   */
  @Test
  public void shouldNotAnonymizeWhenLoansWithOpenFeesAndFinesCloseAndSettingsOfNeverAnonymizeLoansWithFeesAndFines()
    throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {

    LoanHistoryConfigurationBuilder loanHistoryConfig = new LoanHistoryConfigurationBuilder()
      .loanCloseAnonymizeImmediately()
      .feeFineCloseAnonymizeNever();
    createConfiguration(loanHistoryConfig);

    IndividualResource loanResource = loansFixture.checkOutByBarcode(new CheckOutByBarcodeRequestBuilder().forItem(item1)
      .to(user)
      .at(servicePoint.getId()));
    UUID loanID = loanResource.getId();

    createOpenAccountWithFeeFines(loanResource);

    loansFixture.checkInByBarcode(item1);

    anonymizeLoansInTenant();

    assertThat(loansStorageClient.getById(loanID).getJson(),
      not(isAnonymized()));
  }

  /**
   *     Given:
   *         An Anonymize closed loans setting of "Immediately after loan closes"
   *         An Anonymize closed loans with associated fees/fines setting of "Never"
   *         An open loan with an associated fee/fine
   *     When all fees/fines associated with the loan are closed
   *     Then do not anonymize the loan
   */
  @Test
  public void shouldNotAnonymizeOpenLoansWhenFeesAndFinesCloseAndSettingsOfNeverAnonymizeLoansWithFeesAndFines()
    throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {

    LoanHistoryConfigurationBuilder loanHistoryConfig = new LoanHistoryConfigurationBuilder()
      .loanCloseAnonymizeImmediately()
      .feeFineCloseAnonymizeNever();
    createConfiguration(loanHistoryConfig);

    IndividualResource loanResource = loansFixture.checkOutByBarcode(new CheckOutByBarcodeRequestBuilder().forItem(item1)
      .to(user)
      .at(servicePoint.getId()));
    UUID loanID = loanResource.getId();

    createClosedAccountWithFeeFines(loanResource, DateTime.now());

    anonymizeLoansInTenant();

    assertThat(loansStorageClient.getById(loanID).getJson(),
      not(isAnonymized()));
  }

  /**
   *     Given:
   *         An Anonymize closed loans setting of "Immediately"
   *         An Anonymize closed loans with associated fees/fines setting of "X interval after fee/fine closes"
   *         An open loan with an associated fee/fine
   *     When the item in the loan is checked in
   *     Then do not anonymize the loan
   */
  @Test
  public void shouldNotAnonymizeClosedLoansWithClosedFeesAndFinesWhenAnonymizationIntervalForLoansWithFeesAndFinesHasNotPassed()
    throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {

    LoanHistoryConfigurationBuilder loanHistoryConfig = new LoanHistoryConfigurationBuilder()
      .loanCloseAnonymizeImmediately()
      .feeFineCloseAnonymizeAfterXInterval(1, "minute");
    createConfiguration(loanHistoryConfig);

    IndividualResource loanResource = loansFixture.checkOutByBarcode(new CheckOutByBarcodeRequestBuilder().forItem(item1)
      .to(user)
      .at(servicePoint.getId()));
    UUID loanID = loanResource.getId();

    createClosedAccountWithFeeFines(loanResource, DateTime.now());

    loansFixture.checkInByBarcode(item1);

    anonymizeLoansInTenant();

    assertThat(loansStorageClient.getById(loanID).getJson(),
      not(isAnonymized()));
  }

  /**
   *
   *     Given:
   *         An Anonymize closed loans setting of "Immediately"
   *         An Anonymize closed loans with associated fees/fines setting of "X interval after fee/fine closes"
   *         A closed loan with an associated fee/fine
   *     When all fees/fines associated with the loan are closed, and X interval has elapsed after the fees/fines have closed
   *     Then anonymize the loan
   */
  @Test
  public void shouldAnonymizeClosedLoansWhenFeesAndFinesCloseAndAnonymizationIntervalForLoansWithFeesAndFinesHasPassed()
    throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {

    LoanHistoryConfigurationBuilder loanHistoryConfig = new LoanHistoryConfigurationBuilder()
      .loanCloseAnonymizeImmediately()
      .feeFineCloseAnonymizeAfterXInterval(1, "minute");
    createConfiguration(loanHistoryConfig);

    IndividualResource loanResource = loansFixture.checkOutByBarcode(new CheckOutByBarcodeRequestBuilder().forItem(item1)
      .to(user)
      .at(servicePoint.getId()));
    UUID loanID = loanResource.getId();

    createClosedAccountWithFeeFines(loanResource, DateTime.now());

    loansFixture.checkInByBarcode(item1);

    DateTimeUtils.setCurrentMillisOffset(ONE_MINUTE_AND_ONE);
    anonymizeLoansInTenant();

    assertThat(loansStorageClient.getById(loanID).getJson(),
      isAnonymized());
  }
}
