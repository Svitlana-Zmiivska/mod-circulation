package org.folio.circulation.domain.anonymization.config;

import static org.folio.circulation.support.JsonPropertyFetcher.getBooleanProperty;
import static org.folio.circulation.support.JsonPropertyFetcher.getNestedStringProperty;

import com.google.inject.internal.util.Lists;
import java.util.List;
import org.folio.circulation.domain.TimePeriod;

import io.vertx.core.json.JsonObject;
import org.folio.circulation.domain.anonymization.checks.AnonymizationChecker;
import org.folio.circulation.domain.anonymization.checks.IntervalAfterFeesAndFinesCloseChecker;

public class LoanHistoryTenantConfiguration {

  private JsonObject representation;
  private ClosingType closingType;
  private Boolean isTreatEnabled;
  private TimePeriod loanPeriod;

  private LoanHistoryTenantConfiguration(JsonObject representation) {
    this.representation = representation;

    this.closingType = ClosingType.from(getNestedStringProperty(representation,
        "closingType", "loan"));
    this.isTreatEnabled = getBooleanProperty(representation, "treatEnabled");
    this.loanPeriod = new TimePeriod(representation.getJsonObject("loan"));

  }

  public List<AnonymizationChecker> getLoanAnonymizationCheckers() {




    return Lists.newArrayList(new IntervalAfterFeesAndFinesCloseChecker(120));
  }

  public static LoanHistoryTenantConfiguration from(JsonObject jsonObject) {
    return new LoanHistoryTenantConfiguration(jsonObject);
  }

  public JsonObject getRepresentation() {
    return representation;
  }

  public void setRepresentation(JsonObject representation) {
    this.representation = representation;
  }

  public ClosingType getClosingType() {
    return closingType;
  }

  public void setClosingType(ClosingType closingType) {
    this.closingType = closingType;
  }

  public Boolean getTreatEnabled() {
    return isTreatEnabled;
  }

  public void setTreatEnabled(Boolean treatEnabled) {
    isTreatEnabled = treatEnabled;
  }

  public TimePeriod getLoanPeriod() {
    return loanPeriod;
  }
}
