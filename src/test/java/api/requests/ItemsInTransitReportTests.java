package api.requests;

import static api.support.JsonCollectionAssistant.getRecordById;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import api.support.APITests;
import api.support.builders.CheckInByBarcodeRequestBuilder;
import api.support.builders.ItemBuilder;
import api.support.builders.RequestBuilder;
import api.support.fixtures.ItemExamples;
import api.support.http.InventoryItemResource;
import api.support.http.ResourceClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;

import org.folio.circulation.domain.ItemStatus;
import org.folio.circulation.support.http.client.IndividualResource;

public class ItemsInTransitReportTests extends APITests {

  private static final String NAME = "name";
  private static final String CODE = "code";
  private static final String LIBRARY = "libraryName";
  private static final String STATUS_KEY = "status";
  private static final String BARCODE_KEY = "barcode";
  private static final String TITLE = "title";
  private static final String CONTRIBUTORS = "contributors";
  private static final String DESTINATION_SERVICE_POINT = "inTransitDestinationServicePointId";
  private static final String REQUEST_TYPE = "requestType";
  private static final String REQUEST_CREATION_DATE = "requestDate";
  private static final String REQUEST_EXPIRATION_DATE = "requestExpirationDate";
  private static final String REQUEST_PICKUP_SERVICE_POINT_NAME = "requestPickupServicePointName";
  private static final String REQUEST_PATRON_GROUP = "requestPatronGroup";
  private static final String TAGS = "tags";
  private static final String CHECK_IN_SERVICE_POINT = "checkInServicePoint";
  private static final String CHECK_IN_DATE_TIME = "checkInDateTime";
  private static final String DISCOVERY_DISPLAY_NAME = "discoveryDisplayName";
  private static final String PICKUP_LOCATION = "pickupLocation";
  private static final String REQUEST = "request";
  private static final String CALL_NUMBER = "callNumber";
  private static final String ITEM_LEVEL_CALL_NUMBER = "itemLevelCallNumber";
  private static final String ENUMERATION = "enumeration";
  private static final String VOLUME = "volume";
  private static final String YEAR_CAPTION = "yearCaption";
  private static final String SERVICE_POINT_NAME_1 = "Circ Desk 1";
  private static final String SERVICE_POINT_NAME_2 = "Circ Desk 2";
  private static final String REQUEST_PATRON_GROUP_DESCRIPTION = "Regular group";
  private static final String SERVICE_POINT_CODE_2 = "cd2";

  @Test
  public void reportIsEmptyWhenThereAreNoItemsInTransit()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    List<JsonObject> items = ResourceClient.forItemsInTransitReport(client).getAll();

    assertTrue(items.isEmpty());
  }

  @Test
  public void reportIncludesItemInTransit()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    final InventoryItemResource smallAngryPlanet = createSmallAngryPlanet();
    final IndividualResource steve = usersFixture.steve();
    final UUID firstServicePointId = servicePointsFixture.cd1().getId();
    final UUID secondServicePointId = servicePointsFixture.cd2().getId();
    final DateTime checkInDate = new DateTime(2019, 8, 13, 5, 0);
    final DateTime requestDate = new DateTime(2019, 7, 5, 10, 0);
    final LocalDate requestExpirationDate = new LocalDate(2019, 7, 11);

    loansFixture.checkOutByBarcode(smallAngryPlanet);
    createRequest(smallAngryPlanet, steve, secondServicePointId, requestDate, requestExpirationDate);
    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(smallAngryPlanet)
      .on(checkInDate)
      .at(firstServicePointId));

    List<JsonObject> items = ResourceClient.forItemsInTransitReport(client).getAll();

    assertThat(items.size(), is(1));
    JsonObject itemJson = items.get(0);
    verifyItem(itemJson, smallAngryPlanet, secondServicePointId);
    verifyLocation(itemJson);
    verifyRequestWithSecondPickupServicePoint(itemJson, requestDate, requestExpirationDate);
    verifyLoanInFirstServicePoint(itemJson, checkInDate);
  }

  @Test
  public void reportIncludesMultipleDifferentItemsInTransit()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    final InventoryItemResource smallAngryPlanet = createSmallAngryPlanet();
    final InventoryItemResource nod = createNod();

    final IndividualResource steve = usersFixture.steve();
    final IndividualResource rebecca = usersFixture.rebecca();

    final UUID firsServicePointId = servicePointsFixture.cd1().getId();
    final UUID secondServicePointId = servicePointsFixture.cd2().getId();

    final DateTime checkInDate1 = new DateTime(2019, 8, 13, 5, 0);
    final DateTime checkInDate2 = new DateTime(2019, 4, 3, 2, 10);
    final DateTime requestDate1 = new DateTime(2019, 7, 5, 10, 0);
    final LocalDate requestExpirationDate1 = new LocalDate(2019, 7, 11);

    final DateTime requestDate2 = new DateTime(2019, 10, 8, 11, 0);
    final LocalDate requestExpirationDate2 = new LocalDate(2020, 1, 12);

    loansFixture.checkOutByBarcode(smallAngryPlanet);
    loansFixture.checkOutByBarcode(nod);

    createRequest(smallAngryPlanet, steve, secondServicePointId, requestDate1, requestExpirationDate1);
    createRequest(nod, rebecca, secondServicePointId, requestDate2, requestExpirationDate2);

    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(smallAngryPlanet)
      .on(checkInDate1)
      .at(firsServicePointId));
    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(nod)
      .on(checkInDate2)
      .at(firsServicePointId));

    List<JsonObject> items = ResourceClient.forItemsInTransitReport(client).getAll();

    assertThat(items.size(), is(2));
    JsonObject firstItemJson = getRecordById(items, smallAngryPlanet.getId()).get();
    verifyItem(firstItemJson, smallAngryPlanet, secondServicePointId);
    verifyLocation(firstItemJson);
    verifyRequestWithSecondPickupServicePoint(firstItemJson, requestDate1, requestExpirationDate1);
    verifyLoanInFirstServicePoint(firstItemJson, checkInDate1);

    JsonObject secondItemJson = getRecordById(items, nod.getId()).get();
    verifyItem(secondItemJson, nod, secondServicePointId);
    verifyLocation(secondItemJson);
    verifyRequest(secondItemJson, requestDate2, requestExpirationDate2, REQUEST_PATRON_GROUP_DESCRIPTION, SERVICE_POINT_NAME_2);
    verifyLoanInFirstServicePoint(secondItemJson, checkInDate2);
  }

  @Test
  public void reportExcludesItemsThatAreNotInTransit()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    final InventoryItemResource smallAngryPlanet = createSmallAngryPlanet();
    final InventoryItemResource nod = createNod();
    final DateTime checkInDate = new DateTime(2019, 8, 13, 5, 0);
    final DateTime requestDate = new DateTime(2019, 7, 5, 10, 0);
    final LocalDate requestExpirationDate = new LocalDate(2019, 7, 11);

    final IndividualResource steve = usersFixture.steve();
    final UUID firstServicePointId = servicePointsFixture.cd1().getId();
    final UUID secondServicePointId = servicePointsFixture.cd2().getId();

    loansFixture.checkOutByBarcode(smallAngryPlanet);
    loansFixture.checkOutByBarcode(nod);

    createRequest(smallAngryPlanet, steve, secondServicePointId, requestDate, requestExpirationDate);

    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(smallAngryPlanet)
      .on(checkInDate)
      .at(firstServicePointId));

    List<JsonObject> items = ResourceClient.forItemsInTransitReport(client).getAll();

    assertThat(items.size(), is(1));
    JsonObject itemJson = items.get(0);
    verifyItem(itemJson, smallAngryPlanet, secondServicePointId);
    verifyLocation(itemJson);
    verifyRequestWithSecondPickupServicePoint(itemJson, requestDate, requestExpirationDate);
    verifyLoanInFirstServicePoint(itemJson, checkInDate);
  }

  @Test
  public void reportIncludesItemsInTransitToDifferentServicePoints()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    final InventoryItemResource smallAngryPlanet = createSmallAngryPlanet();
    final InventoryItemResource nod = createNod();

    final IndividualResource steve = usersFixture.steve();
    final IndividualResource rebecca = usersFixture.rebecca();

    final UUID firstServicePointId = servicePointsFixture.cd1().getId();
    final UUID secondServicePointId = servicePointsFixture.cd2().getId();

    final DateTime checkInDate1 = new DateTime(2019, 8, 13, 5, 0);
    final DateTime checkInDate2 = new DateTime(2019, 4, 3, 2, 10);
    final DateTime requestDate1 = new DateTime(2019, 7, 5, 10, 0);
    final DateTime requestDate2 = new DateTime(2019, 10, 8, 11, 0);
    final LocalDate requestExpirationDate1 = new LocalDate(2019, 7, 11);
    final LocalDate requestExpirationDate2 = new LocalDate(2020, 1, 12);

    loansFixture.checkOutByBarcode(smallAngryPlanet);
    loansFixture.checkOutByBarcode(nod);

    createRequest(smallAngryPlanet, steve, firstServicePointId, requestDate1, requestExpirationDate1);
    createRequest(nod, rebecca, secondServicePointId, requestDate2, requestExpirationDate2);

    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(smallAngryPlanet)
      .on(checkInDate1)
      .at(secondServicePointId));
    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(nod)
      .on(checkInDate2)
      .at(firstServicePointId));

    List<JsonObject> items = ResourceClient.forItemsInTransitReport(client).getAll();

    assertThat(items.size(), is(2));
    JsonObject firstItemJson = getRecordById(items, smallAngryPlanet.getId()).get();
    verifyItem(firstItemJson, smallAngryPlanet, firstServicePointId);
    verifyLocation(firstItemJson);
    verifyRequest(firstItemJson, requestDate1, requestExpirationDate1, REQUEST_PATRON_GROUP_DESCRIPTION, SERVICE_POINT_NAME_1);
    verifyLoan(firstItemJson, checkInDate1, SERVICE_POINT_NAME_2,
      SERVICE_POINT_CODE_2, "Circulation Desk -- Back Entrance");

    JsonObject secondItemJson = getRecordById(items, nod.getId()).get();
    verifyItem(secondItemJson, nod, secondServicePointId);
    verifyLocation(secondItemJson);
    verifyRequest(secondItemJson, requestDate2, requestExpirationDate2, REQUEST_PATRON_GROUP_DESCRIPTION, SERVICE_POINT_NAME_2);
    verifyLoanInFirstServicePoint(secondItemJson, checkInDate2);
  }

  @Test
  public void reportIncludesItemsInTransitWithMoreThanOneOpenRequestInQueue()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    final InventoryItemResource smallAngryPlanet = createSmallAngryPlanet();
    final InventoryItemResource nod = createNod();

    final IndividualResource steve = usersFixture.steve();
    final IndividualResource rebecca = usersFixture.rebecca();

    final UUID firstServicePointId = servicePointsFixture.cd1().getId();
    final UUID secondServicePointId = servicePointsFixture.cd2().getId();

    final DateTime checkInDate1 = new DateTime(2019, 8, 13, 5, 0);
    final DateTime checkInDate2 = new DateTime(2019, 4, 3, 2, 10);

    final DateTime requestSmallAngryPlanetDate1 = new DateTime(2019, 7, 5, 10, 0);
    final DateTime requestSmallAngryPlanetDate2 = new DateTime(2019, 10, 1, 12, 0);
    final LocalDate requestSmallAngryPlanetExpirationDate1 = new LocalDate(2019, 7, 11);
    final LocalDate requestSmallAngryPlanetExpirationDate2 = new LocalDate(2019, 11, 12);

    final DateTime requestNodeDate1 = new DateTime(2019, 5, 11, 1, 0);
    final DateTime requestNodeDate2 = new DateTime(2019, 10, 8, 11, 0);
    final LocalDate requestNodeExpirationDate1 = new LocalDate(2020, 1, 12);
    final LocalDate requestNodeExpirationDate2 = new LocalDate(2020, 10, 10);

    loansFixture.checkOutByBarcode(smallAngryPlanet);
    loansFixture.checkOutByBarcode(nod);

    createRequest(smallAngryPlanet, steve, firstServicePointId, requestSmallAngryPlanetDate1, requestSmallAngryPlanetExpirationDate1);
    createRequest(smallAngryPlanet, rebecca, firstServicePointId, requestSmallAngryPlanetDate2, requestSmallAngryPlanetExpirationDate2);

    createRequest(nod, rebecca, secondServicePointId, requestNodeDate1, requestNodeExpirationDate1);
    createRequest(nod, steve, secondServicePointId, requestNodeDate2, requestNodeExpirationDate2);

    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(smallAngryPlanet)
      .on(checkInDate1)
      .at(secondServicePointId));
    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(nod)
      .on(checkInDate2)
      .at(firstServicePointId));

    List<JsonObject> items = ResourceClient.forItemsInTransitReport(client).getAll();

    assertThat(items.size(), is(2));
    JsonObject firstItemJson = getRecordById(items, smallAngryPlanet.getId()).get();
    verifyItem(firstItemJson, smallAngryPlanet, firstServicePointId);
    verifyLocation(firstItemJson);
    verifyRequest(firstItemJson, requestSmallAngryPlanetDate1, requestSmallAngryPlanetExpirationDate1, REQUEST_PATRON_GROUP_DESCRIPTION, SERVICE_POINT_NAME_1);
    verifyLoan(firstItemJson, checkInDate1, SERVICE_POINT_NAME_2,
      SERVICE_POINT_CODE_2, "Circulation Desk -- Back Entrance");

    JsonObject secondItemJson = getRecordById(items, nod.getId()).get();
    verifyItem(secondItemJson, nod, secondServicePointId);
    verifyLocation(secondItemJson);
    verifyRequest(secondItemJson, requestNodeDate1, requestNodeExpirationDate1, REQUEST_PATRON_GROUP_DESCRIPTION, SERVICE_POINT_NAME_2);
    verifyLoanInFirstServicePoint(secondItemJson, checkInDate2);
  }

  @Test
  public void reportIncludesItemsInTransitWithEmptyRequestQueue()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    final InventoryItemResource smallAngryPlanet = createSmallAngryPlanet();
    final InventoryItemResource nod = createNod();
    final UUID firsServicePointId = servicePointsFixture.cd1().getId();
    final UUID secondServicePointId = servicePointsFixture.cd2().getId();

    final DateTime checkInDate1 = new DateTime(2019, 8, 13, 5, 0);
    final DateTime checkInDate2 = new DateTime(2019, 4, 3, 2, 10);

    final String checkInServicePointDiscoveryName = "Circulation Desk -- Back Entrance";

    loansFixture.checkOutByBarcode(smallAngryPlanet);
    loansFixture.checkOutByBarcode(nod);

    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(smallAngryPlanet)
      .on(checkInDate1)
      .at(secondServicePointId));
    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(nod)
      .on(checkInDate2)
      .at(secondServicePointId));

    List<JsonObject> items = ResourceClient.forItemsInTransitReport(client).getAll();

    assertThat(items.size(), is(2));
    JsonObject firstItemJson = getRecordById(items, smallAngryPlanet.getId()).get();
    verifyItem(firstItemJson, smallAngryPlanet, firsServicePointId);
    verifyLocation(firstItemJson);
    assertNull(firstItemJson.getMap().get(REQUEST));
    verifyLoan(firstItemJson, checkInDate1, SERVICE_POINT_NAME_2, SERVICE_POINT_CODE_2, checkInServicePointDiscoveryName);

    JsonObject secondItemJson = getRecordById(items, nod.getId()).get();
    verifyItem(secondItemJson, nod, firsServicePointId);
    verifyLocation(secondItemJson);
    assertNull(secondItemJson.getMap().get(REQUEST));
    verifyLoan(secondItemJson, checkInDate2, SERVICE_POINT_NAME_2, SERVICE_POINT_CODE_2, checkInServicePointDiscoveryName);
  }

  @Test
  public void reportItemsInTransitSortedByCheckInServicePoint()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    final InventoryItemResource smallAngryPlanet = createSmallAngryPlanet();
    final InventoryItemResource nod = createNod();
    final InventoryItemResource smallAngryPlanetWithFourthCheckInServicePoint = itemsFixture
      .basedUponSmallAngryPlanet(createSmallAngryPlanetItemBuilder()
        .withBarcode("34"), itemsFixture.thirdFloorHoldings());
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource rebecca = usersFixture.rebecca();
    final UUID firstServicePointId = servicePointsFixture.cd1().getId();
    final UUID secondServicePointId = servicePointsFixture.cd2().getId();
    final UUID fourthServicePointId = servicePointsFixture.cd4().getId();
    final DateTime checkInDate1 = new DateTime(2019, 8, 13, 5, 0);
    final DateTime checkInDate2 = new DateTime(2019, 4, 3, 2, 10);
    final DateTime checkInDate3 = new DateTime(2019, 10, 10, 3, 0);
    final DateTime requestDate1 = new DateTime(2019, 7, 5, 10, 0);
    final DateTime requestDate2 = new DateTime(2019, 10, 8, 11, 0);
    final LocalDate requestExpirationDate1 = new LocalDate(2019, 7, 11);
    final LocalDate requestExpirationDate2 = new LocalDate(2020, 1, 12);

    loansFixture.checkOutByBarcode(smallAngryPlanet);
    loansFixture.checkOutByBarcode(nod);
    loansFixture.checkOutByBarcode(smallAngryPlanetWithFourthCheckInServicePoint);

    createRequest(smallAngryPlanet, steve, firstServicePointId, requestDate1, requestExpirationDate1);
    createRequest(nod, rebecca, secondServicePointId, requestDate2, requestExpirationDate2);

    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(smallAngryPlanetWithFourthCheckInServicePoint)
      .on(checkInDate3)
      .at(fourthServicePointId));
    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(nod)
      .on(checkInDate2)
      .at(firstServicePointId));
    loansFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
      .forItem(smallAngryPlanet)
      .on(checkInDate1)
      .at(secondServicePointId));

    List<JsonObject> items = ResourceClient.forItemsInTransitReport(client).getAll();

    assertThat(items.size(), is(3));

    JsonObject firstItemJson = items.get(0);
    verifyItem(firstItemJson, nod, secondServicePointId);
    verifyLocation(firstItemJson);
    verifyRequest(firstItemJson, requestDate2, requestExpirationDate2, REQUEST_PATRON_GROUP_DESCRIPTION, SERVICE_POINT_NAME_2);
    verifyLoanInFirstServicePoint(firstItemJson, checkInDate2);

    JsonObject secondItemJson = items.get(1);
    verifyItem(secondItemJson, smallAngryPlanet, firstServicePointId);
    verifyLocation(secondItemJson);
    verifyRequest(secondItemJson, requestDate1, requestExpirationDate1, REQUEST_PATRON_GROUP_DESCRIPTION, SERVICE_POINT_NAME_1);
    verifyLoan(secondItemJson, checkInDate1, SERVICE_POINT_NAME_2,
      SERVICE_POINT_CODE_2, "Circulation Desk -- Back Entrance");

    JsonObject thirdItemJson = items.get(2);
    verifyItem(thirdItemJson, smallAngryPlanetWithFourthCheckInServicePoint, firstServicePointId);
    verifyLocation(thirdItemJson);
    verifyLoan(thirdItemJson, checkInDate3, "Circ Desk 4",
      "cd4", "Circulation Desk -- Basement");
  }

  private void createRequest(InventoryItemResource smallAngryPlanet,
                             IndividualResource steve, UUID secondServicePointId,
                             DateTime requestDate, LocalDate requestExpirationDate)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {
    RequestBuilder.Tags tags = new RequestBuilder.Tags(Arrays.asList("tag1", "tag2"));
    RequestBuilder secondRequestBuilderOnItem = new RequestBuilder()
      .open()
      .hold()
      .withPickupServicePointId(secondServicePointId)
      .forItem(smallAngryPlanet)
      .withTags(tags)
      .withRequestDate(requestDate)
      .withRequestExpiration(requestExpirationDate)
      .by(steve);

    requestsClient.create(secondRequestBuilderOnItem);
  }

  private void verifyItem(JsonObject itemJson, InventoryItemResource item,
                          UUID secondServicePointId) {
    assertThat(itemJson.getString(BARCODE_KEY), is(item.getBarcode()));
    assertThat(itemJson.getJsonObject(STATUS_KEY).getMap().get(NAME),
      is(ItemStatus.IN_TRANSIT.getValue()));
    assertThat(itemJson.getString(DESTINATION_SERVICE_POINT), is(String.valueOf(secondServicePointId)));
    final JsonObject smallAngryPlanetInstance = item.getInstance().getJson();
    assertThat(itemJson.getString(TITLE), is(smallAngryPlanetInstance.getString(TITLE)));
    final String contributors = String.valueOf(((JsonArray) smallAngryPlanetInstance
      .getMap().get(CONTRIBUTORS)).getJsonObject(0).getMap().get(NAME));
    assertThat(itemJson.getJsonArray(CONTRIBUTORS)
      .getJsonObject(0).getMap().get(NAME), is(contributors));
    final JsonObject smallAngryPlanetResponse = item.getResponse().getJson();
    assertThat(itemJson.getString(CALL_NUMBER), is(smallAngryPlanetResponse.getString(ITEM_LEVEL_CALL_NUMBER)));
    assertThat(itemJson.getString(ENUMERATION), is(smallAngryPlanetResponse.getString(ENUMERATION)));
    assertThat(itemJson.getString(VOLUME), is(smallAngryPlanetResponse.getString(VOLUME)));
    assertThat(itemJson.getJsonArray(YEAR_CAPTION), is(smallAngryPlanetResponse.getJsonArray(YEAR_CAPTION)));
  }

  private void verifyLocation(JsonObject itemJson) {
    Map<String, String> actualLocation = (Map<String, String>) itemJson.getMap().get("location");
    assertThat(actualLocation.get(NAME), is("3rd Floor"));
    assertThat(actualLocation.get(CODE), is("NU/JC/DL/3F"));
    assertThat(actualLocation.get(LIBRARY), is("Djanogly Learning Resource Centre"));
  }

  private void verifyRequest(JsonObject itemJson, DateTime requestDate,
                             LocalDate requestExpirationDate, String requestPatronGroup, String pickupServicePoint) {
    Map<String, String> actualRequest = (Map<String, String>) itemJson.getMap().get(REQUEST);
    assertThat(actualRequest.get(REQUEST_TYPE), is("Hold"));
    assertThat(actualRequest.get(REQUEST_PATRON_GROUP), is(requestPatronGroup));
    assertThat(actualRequest.get(REQUEST_CREATION_DATE), is(String.valueOf(requestDate)));
    assertThat(actualRequest.get(REQUEST_EXPIRATION_DATE), is(requestExpirationDate.toDateTimeAtStartOfDay().toString()));
    assertThat(actualRequest.get(REQUEST_PICKUP_SERVICE_POINT_NAME), is(pickupServicePoint));
    assertThat(actualRequest.get(TAGS), is(Arrays.asList("tag1", "tag2")));
  }

  private void verifyRequestWithSecondPickupServicePoint(JsonObject itemJson, DateTime requestDate,
                                                         LocalDate requestExpirationDate) {

    verifyRequest(itemJson, requestDate, requestExpirationDate, REQUEST_PATRON_GROUP_DESCRIPTION, SERVICE_POINT_NAME_2);
  }

  private void verifyLoan(JsonObject itemJson, DateTime checkInDate,
                          String checkInServicePointName, String checkInServicePointCode,
                          String checkInServicePointDiscoveryName) {
    Map<String, Object> actualLoan = (Map<String, Object>) itemJson.getMap().get("loan");
    assertThat(actualLoan.get(CHECK_IN_DATE_TIME), is(String.valueOf(checkInDate)));
    Map<String, String> actualCheckInServicePoint = (Map<String, String>) actualLoan.get(CHECK_IN_SERVICE_POINT);
    assertThat(actualCheckInServicePoint.get(NAME), is(checkInServicePointName));
    assertThat(actualCheckInServicePoint.get(CODE), is(checkInServicePointCode));
    assertThat(actualCheckInServicePoint.get(DISCOVERY_DISPLAY_NAME), is(checkInServicePointDiscoveryName));
    assertThat(actualCheckInServicePoint.get(PICKUP_LOCATION), is(Boolean.TRUE));
  }

  private void verifyLoanInFirstServicePoint(JsonObject itemJson, DateTime checkInDate) {
    verifyLoan(itemJson, checkInDate, SERVICE_POINT_NAME_1,
      "cd1", "Circulation Desk -- Hallway");
  }

  private InventoryItemResource createNod() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException {
    final ItemBuilder nodItemBuilder = ItemExamples.basedUponNod(
      materialTypesFixture.book().getId(),
      loanTypesFixture.canCirculate().getId())
      .withEnumeration("nodeEnumeration")
      .withVolume("nodeVolume")
      .withYearCaption(Arrays.asList("2017"))
      .withCallNumber("222245", null, null);
    return itemsFixture.basedUponNod(builder -> nodItemBuilder);
  }

  private InventoryItemResource createSmallAngryPlanet() throws MalformedURLException,
    InterruptedException, ExecutionException, TimeoutException {
    final ItemBuilder smallAngryPlanetItemBuilder = createSmallAngryPlanetItemBuilder();
    return itemsFixture.basedUponSmallAngryPlanet(smallAngryPlanetItemBuilder, itemsFixture.thirdFloorHoldings());
  }

  private ItemBuilder createSmallAngryPlanetItemBuilder() throws MalformedURLException,
    InterruptedException, ExecutionException, TimeoutException {
    return ItemExamples.basedUponSmallAngryPlanet(
      materialTypesFixture.book().getId(),
      loanTypesFixture.canCirculate().getId(),
      StringUtils.EMPTY,
      "ItemPrefix",
      "ItemSuffix",
      Collections.singletonList(""))
      .withEnumeration("smallAngryPlanetEnumeration")
      .withVolume("smallAngryPlanetVolume")
      .withYearCaption(Arrays.asList("2019"))
      .withCallNumber("55555", null, null);
  }
}
