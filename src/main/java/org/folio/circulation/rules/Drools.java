package org.folio.circulation.rules;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message.Level;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Holds a Drools kieSession to calculate a loan policy.
 */
public class Drools {
  // https://docs.jboss.org/drools/release/6.2.0.CR1/drools-docs/html/ch19.html
  // http://www.deepakgaikwad.net/index.php/2016/05/16/drools-tutorial-beginners.html

  private Match match = new Match();
  private KieContainer kieContainer;

  /**
   * Create the Drools kieSession based on a String containing a drools file.
   * @param drools A file in Drools syntax with the circulation rules.
   */
  public Drools(String drools) {
    KieServices kieServices = KieServices.Factory.get();
    KieFileSystem kfs = kieServices.newKieFileSystem();
    kfs.write("src/main/resources/circulationrules/circulation-rules.drl", drools);
    KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
    kieBuilder.buildAll();
    if (kieBuilder.getResults().hasMessages(Level.ERROR)) {
      throw new IllegalArgumentException("Drools build errors:\n" + kieBuilder.getResults().toString());
    }
    kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
  }

  private KieSession createSession(String itemType, String loanType, String patronGroup, String shelvingLocation) {
    KieSession kieSession = kieContainer.newKieSession();
    match.loanPolicyId = null;
    kieSession.setGlobal("match", match);
    kieSession.insert(new ItemType(itemType));
    kieSession.insert(new LoanType(loanType));
    kieSession.insert(new PatronGroup(patronGroup));
    kieSession.insert(new ShelvingLocation(shelvingLocation));
    kieSession.insert(new CampusLocation(""));
    kieSession.insert(new BranchLocation(""));
    kieSession.insert(new CollectionLocation(""));
    return kieSession;
  }

  /**
   * Calculate the loan policy for itemTypeName and loanTypeName.
   * @param itemType the name of the item type
   * @param loanType the name of the loan type
   * @param patronGroup group the patron belongs to
   * @param shelvingLocation - item's shelving location
   * @return the name of the loan policy
   */
  public String loanPolicy(String itemType, String loanType, String patronGroup, String shelvingLocation) {
    KieSession kieSession = createSession(itemType, loanType, patronGroup, shelvingLocation);
    kieSession.fireAllRules();
    kieSession.dispose();
    return match.loanPolicyId;
  }

  /**
   * Calculate the request policy for itemTypeName and requestTypeName.
   * @param itemType the name of the item type
   * @param requestType the name of the request type
   * @param patronGroup group the patron belongs to
   * @param shelvingLocation - item's shelving location
   * @return the name of the request policy
   */
  public String requestPolicy(String itemType, String requestType, String patronGroup, String shelvingLocation) {
    KieSession kieSession = createSession(itemType, requestType, patronGroup, shelvingLocation);
    kieSession.fireAllRules();
    kieSession.dispose();
    return match.requestPolicyId;
  }

  /**
   * Return all loan policies calculated using the drools rules
   * in the order they match.
   * @param itemType the item's material type
   * @param loanType the item's loan type
   * @param patronGroup group the patron belongs to
   * @param shelvingLocation - item's shelving location
   * @return matches, each match has a loanPolicyId and a circulationRuleLine field
   */
  public JsonArray loanPolicies(String itemType, String loanType, String patronGroup, String shelvingLocation) {
    KieSession kieSession = createSession(itemType, loanType, patronGroup, shelvingLocation);
    JsonArray array = new JsonArray();
    while (kieSession.fireAllRules() > 0) {
      JsonObject json = new JsonObject();
      json.put("loanPolicyId", match.loanPolicyId);
      json.put("circulationRuleLine", match.lineNumber);
      array.add(json);
    }
    kieSession.dispose();
    return array;
  }

   /**
   * Return all request policies calculated using the drools rules
   * in the order they match.
   * @param itemType the item's material type
   * @param requestType the item's request type
   * @param patronGroup group the patron belongs to
   * @param shelvingLocation - item's shelving location
   * @return matches, each match has a requestPolicyId and a circulationRuleLine field
   */
  public JsonArray requestPolicies(String itemType, String requestType, String patronGroup, String shelvingLocation) {
    KieSession kieSession = createSession(itemType, requestType, patronGroup, shelvingLocation);
    JsonArray array = new JsonArray();
    while (kieSession.fireAllRules() > 0) {
      JsonObject json = new JsonObject();
      json.put("requestPolicyId", match.requestPolicyId);
      json.put("circulationRuleLine", match.lineNumber);
      array.add(json);
    }
    kieSession.dispose();
    return array;
  }

  /**
   * Return the loan policy calculated using the drools rules and the item type and loan type.
   * @param droolsFile - rules to use
   * @param itemType - item (material) type name
   * @param loanType - loan type name
   * @param patronGroup group the patron belongs to
   * @param shelvingLocation - item's shelving location
   * @return loan policy
   */
  public static String loanPolicy(String droolsFile, String itemType, String loanType, String patronGroup, String shelvingLocation) {
    return new Drools(droolsFile).loanPolicy(itemType, loanType, patronGroup, shelvingLocation);
  }

  /**
   * Return the request policy calculated using the drools rules and the item type and request type.
   * @param droolsFile - rules to use
   * @param itemType - item (material) type name
   * @param requestType - request type name
   * @param patronGroup group the patron belongs to
   * @param shelvingLocation - item's shelving location
   * @return request policy
   */
  public static String requestPolicy(String droolsFile, String itemType, String requestType, String patronGroup, String shelvingLocation) {
    return new Drools(droolsFile).requestPolicy(itemType, requestType, patronGroup, shelvingLocation);
  }
}
