package api.requests;

import static org.folio.circulation.support.JsonPropertyFetcher.getNestedStringProperty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.folio.circulation.domain.RequestType;
import org.folio.circulation.support.http.client.IndividualResource;
import org.folio.circulation.support.http.client.Response;
import org.folio.circulation.support.http.client.ResponseHandler;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import api.support.APITests;
import api.support.builders.RequestBuilder;
import api.support.http.InterfaceUrls;
import io.vertx.core.json.JsonObject;

public class InstanceRequestsAPICreationTests extends APITests {

  @Test
  public void canCreateATitleLevelRequestForMultipleAvailableItemsAndAMatchingPickupLocationId()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    UUID pickupServicePointId = servicePointsFixture.cd1().getId();
    UUID requesterId = usersFixture.jessica().getId();
    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);
    DateTime requestExpirationDate = requestDate.plusDays(30);

    IndividualResource instanceMultipleCopies = instancesFixture.basedUponDunkirk();
    IndividualResource holdings = holdingsFixture.defaultWithHoldings(instanceMultipleCopies.getId());

    IndividualResource locationsResource = locationsFixture.mainFloor();

    itemsFixture.basedUponDunkirkWithCustomHoldingAndLocation(holdings.getId(), null);
    final IndividualResource item2 = itemsFixture.basedUponDunkirkWithCustomHoldingAndLocation(holdings.getId(), locationsResource.getId());
    itemsFixture.basedUponDunkirkWithCustomHoldingAndLocation(holdings.getId(), null);

    JsonObject requestBody = createInstanceRequestObject(instanceMultipleCopies.getId(), requesterId,
                                            pickupServicePointId, requestDate, requestExpirationDate);

    CompletableFuture<Response> postCompleted = new CompletableFuture<>();

    client.post(InterfaceUrls.requestsUrl("/instances"), requestBody,
      ResponseHandler.any(postCompleted));

    Response postResponse = postCompleted.get(10, TimeUnit.SECONDS);

    JsonObject representation = postResponse.getJson();
    validateInstanceRequestResponse(representation,
      pickupServicePointId,
      instanceMultipleCopies.getId(),
      item2.getId(),
      RequestType.PAGE);
  }

  @Test
  public void canCreateATitleLevelRequestForOneAvailableCopy()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    UUID pickupServicePointId = servicePointsFixture.cd1().getId();
    UUID requesterId = usersFixture.jessica().getId();
    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);
    DateTime requestExpirationDate = requestDate.plusDays(30);

    IndividualResource instance = instancesFixture.basedUponDunkirk();
    IndividualResource holdings = holdingsFixture.defaultWithHoldings(instance.getId());

    final IndividualResource item = itemsFixture.basedUponDunkirkWithCustomHoldingAndLocation(holdings.getId(), null);

    JsonObject requestBody = createInstanceRequestObject(instance.getId(), requesterId,
      pickupServicePointId, requestDate, requestExpirationDate);

    CompletableFuture<Response> postCompleted = new CompletableFuture<>();

    client.post(InterfaceUrls.requestsUrl("/instances"), requestBody,
      ResponseHandler.any(postCompleted));

    Response postResponse = postCompleted.get(10, TimeUnit.SECONDS);

    JsonObject representation = postResponse.getJson();
    validateInstanceRequestResponse(representation,
      pickupServicePointId,
      instance.getId(),
      item.getId(),
      RequestType.PAGE);

  }

  @Test
  public void canCreateATitleLevelRequestForMultipleAvailableItemsWithNoMatchingPickupLocationIds()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    UUID pickupServicePointId = servicePointsFixture.cd1().getId();
    UUID requesterId = usersFixture.jessica().getId();
    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);
    DateTime requestExpirationDate = requestDate.plusDays(30);

    IndividualResource instance = instancesFixture.basedUponDunkirk();
    IndividualResource holdings = holdingsFixture.defaultWithHoldings(instance.getId());

    //create 3 copies with no location id's assigned.
    itemsFixture.basedUponDunkirkWithCustomHoldingAndLocation(holdings.getId(), null);
    itemsFixture.basedUponDunkirkWithCustomHoldingAndLocation(holdings.getId(), null);
    itemsFixture.basedUponDunkirkWithCustomHoldingAndLocation(holdings.getId(), null);

    JsonObject requestBody = createInstanceRequestObject(instance.getId(), requesterId,
      pickupServicePointId, requestDate, requestExpirationDate);

    CompletableFuture<Response> postCompleted = new CompletableFuture<>();

    client.post(InterfaceUrls.requestsUrl("/instances"), requestBody,
      ResponseHandler.any(postCompleted));

    Response postResponse = postCompleted.get(10, TimeUnit.SECONDS);

    JsonObject representation = postResponse.getJson();
    validateInstanceRequestResponse(representation,
      pickupServicePointId,
      instance.getId(),
      null,  //although we create the items in certain order, this order may not be what the code picks up when query for items by holdings, so there is no point to check for itemId
      RequestType.PAGE);

  }

  @Test
  public void cannotSuccessfullyPlaceATitleLevelRequestWhenTitleLevelRequestMissingRequiredFields()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    UUID pickupServicePointId = servicePointsFixture.cd1().getId();
    UUID requesterId = usersFixture.jessica().getId();
    DateTime requestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);
    DateTime requestExpirationDate = requestDate.plusDays(30);

    IndividualResource instance = instancesFixture.basedUponDunkirk();
    IndividualResource holdings = holdingsFixture.defaultWithHoldings(instance.getId());

    //create 3 copies with no location id's assigned.
    itemsFixture.basedUponDunkirkWithCustomHoldingAndLocationAndCheckedOut(holdings.getId(), null);
    itemsFixture.basedUponDunkirkWithCustomHoldingAndLocationAndCheckedOut(holdings.getId(), null);
    itemsFixture.basedUponDunkirkWithCustomHoldingAndLocationAndCheckedOut(holdings.getId(), null);

    JsonObject requestBody = createInstanceRequestObject(instance.getId(), requesterId,
      pickupServicePointId, requestDate, requestExpirationDate);
    requestBody.remove("pickupServicePointId");
    requestBody.remove("instanceId");

    CompletableFuture<Response> postCompleted = new CompletableFuture<>();

    client.post(InterfaceUrls.requestsUrl("/instances"), requestBody,
      ResponseHandler.any(postCompleted));

    Response postResponse = postCompleted.get(10, TimeUnit.SECONDS);
    assertEquals(422, postResponse.getStatusCode());

    JsonObject representation = postResponse.getJson();
    assertEquals("Request must have an instance id", representation.getJsonArray("errors")
                                                                            .getJsonObject(0)
                                                                            .getString("message"));
  }

  @Test
  public void canSuccessfullyPlaceATitleLevelRequestWhenNoCopyIsAvailable()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    UUID pickupServicePointId = servicePointsFixture.cd1().getId();

    LocalDate requestDate = new LocalDate(2017, 7, 22);
    LocalDate requestExpirationDate = requestDate.plusDays(30);

    IndividualResource instance = instancesFixture.basedUponDunkirk();
    IndividualResource holdings = holdingsFixture.defaultWithHoldings(instance.getId());

    //create 2 copies with no location id's assigned.
    final IndividualResource item1 = itemsFixture.basedUponDunkirkWithCustomHoldingAndLocationAndCheckedOut(holdings.getId(), null);
    final IndividualResource item2 = itemsFixture.basedUponDunkirkWithCustomHoldingAndLocationAndCheckedOut(holdings.getId(), null);

    //Set up request queues. Item1 has 2 requests (1 queued request), Item2 has 0 queued request. Item2 should be satisfied.
    List<IndividualResource> patrons1 = new ArrayList<>();
    patrons1.add(usersFixture.jessica());
    patrons1.add(usersFixture.james());
    placeHoldRequest(item1, pickupServicePointId, patrons1, requestExpirationDate);

    List<IndividualResource> patrons2 = new ArrayList<>();
    patrons2.add(usersFixture.steve());
    placeHoldRequest(item2, pickupServicePointId, patrons2, requestExpirationDate);

    IndividualResource instanceRequester = usersFixture.charlotte();

    DateTime instanceRequestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);
    DateTime instanceRequestDateRequestExpirationDate = instanceRequestDate.plusDays(30);

    JsonObject requestBody = createInstanceRequestObject(instance.getId(), instanceRequester.getId(),
      pickupServicePointId, instanceRequestDate, instanceRequestDateRequestExpirationDate);

    CompletableFuture<Response> postCompleted = new CompletableFuture<>();

    client.post(InterfaceUrls.requestsUrl("/instances"), requestBody,
      ResponseHandler.any(postCompleted));

    Response postResponse = postCompleted.get(10, TimeUnit.SECONDS);
    assertEquals(201, postResponse.getStatusCode());


    JsonObject representation = postResponse.getJson();
    validateInstanceRequestResponse(representation, pickupServicePointId, instance.getId(), item2.getId(), RequestType.HOLD);
  }

  @Test
  public void canSuccessfullyPlaceATitleLevelRequestOnAvailableCopyWithAdditionalUnavailableCopy()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    UUID pickupServicePointId = servicePointsFixture.cd1().getId();

    LocalDate requestDate = new LocalDate(2017, 7, 22);
    LocalDate requestExpirationDate = requestDate.plusDays(30);

    IndividualResource instance = instancesFixture.basedUponDunkirk();
    IndividualResource holdings = holdingsFixture.defaultWithHoldings(instance.getId());

    //create 2 copies with no location id's assigned.
    final IndividualResource item1 = itemsFixture.basedUponDunkirkWithCustomHoldingAndLocationAndCheckedOut(holdings.getId(), null);
    final IndividualResource item2 = itemsFixture.basedUponDunkirkWithCustomHoldingAndLocation(holdings.getId(), null);

    //Set up request queues. Item1 has requests (1 queued request), Item2 is Avaialble. Item2 should be satisfied.
    List<IndividualResource> patrons1 = new ArrayList<>();
    patrons1.add(usersFixture.jessica());
    patrons1.add(usersFixture.james());
    placeHoldRequest(item1, pickupServicePointId, patrons1, requestExpirationDate);

    IndividualResource instanceRequester = usersFixture.charlotte();

    DateTime instanceRequestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);
    DateTime instanceRequestDateRequestExpirationDate = instanceRequestDate.plusDays(30);

    JsonObject requestBody = createInstanceRequestObject(instance.getId(), instanceRequester.getId(),
      pickupServicePointId, instanceRequestDate, instanceRequestDateRequestExpirationDate);

    CompletableFuture<Response> postCompleted = new CompletableFuture<>();

    client.post(InterfaceUrls.requestsUrl("/instances"), requestBody,
      ResponseHandler.any(postCompleted));

    Response postResponse = postCompleted.get(10, TimeUnit.SECONDS);
    assertEquals(201, postResponse.getStatusCode());

    JsonObject representation = postResponse.getJson();
    validateInstanceRequestResponse(representation, pickupServicePointId, instance.getId(), item2.getId(), RequestType.PAGE);
  }

  @Test
  public void canSuccessfullyPlaceATitleLevelRequestOnUnvailableCopyWhenBothUnavailableCopiesHaveSameQueueLength()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    UUID pickupServicePointId = servicePointsFixture.cd1().getId();

    DateTime instanceRequestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);
    DateTime instanceRequestDateRequestExpirationDate = instanceRequestDate.plusDays(30);

    LocalDate requestDate = new LocalDate(2017, 7, 22);
    LocalDate requestExpirationDate1 = requestDate.plusDays(30);
    LocalDate requestExpirationDate2 = requestDate.minusDays(30);

    IndividualResource instance = instancesFixture.basedUponDunkirk();
    IndividualResource holdings = holdingsFixture.defaultWithHoldings(instance.getId());

    //create 2 copies with no location id's assigned.
    final IndividualResource item1 = itemsFixture.basedUponDunkirkWithCustomHoldingAndLocationAndCheckedOut(holdings.getId(), null);
    final IndividualResource item2 = itemsFixture.basedUponDunkirkWithCustomHoldingAndLocationAndCheckedOut(holdings.getId(), null);

    //Set up request queues. Item1 has requests (1 queued request), Item2 is requests (1 queued), either should be satisfied.
    List<IndividualResource> patrons1 = new ArrayList<>();
    patrons1.add(usersFixture.jessica());
    patrons1.add(usersFixture.james());
    placeHoldRequest(item1, pickupServicePointId, patrons1, requestExpirationDate1);

   List<IndividualResource> patrons2 = new ArrayList<>();
    patrons2.add(usersFixture.steve());
    patrons2.add(usersFixture.rebecca());
    placeHoldRequest(item2, pickupServicePointId, patrons2, requestExpirationDate2);

    IndividualResource instanceRequester = usersFixture.charlotte();

    JsonObject requestBody = createInstanceRequestObject(instance.getId(), instanceRequester.getId(),
      pickupServicePointId, instanceRequestDate, instanceRequestDateRequestExpirationDate);

    CompletableFuture<Response> postCompleted = new CompletableFuture<>();

    client.post(InterfaceUrls.requestsUrl("/instances"), requestBody,
      ResponseHandler.any(postCompleted));

    Response postResponse = postCompleted.get(10, TimeUnit.SECONDS);
    assertEquals(201, postResponse.getStatusCode());

    JsonObject representation = postResponse.getJson();
    //Item2 should have been chosen because it has the nearest requestExpirationDate
    validateInstanceRequestResponse(representation, pickupServicePointId, instance.getId(), item2.getId(), RequestType.HOLD);
  }

  @Test
  public void canPlaceRequestOnUnavailableCopyWhenAnotherCopyHasBeenRequested()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    //The scenario we're checking is if a user has already made a request on an available iemm, hasn't picked it up, but then
    //goes back to place an instance level request. The system will find this item and rejects the request for the user, so the user
    //could be given a successful request on an unavailable item instead, if there is any.
    UUID pickupServicePointId = servicePointsFixture.cd1().getId();

    DateTime instanceRequestDate = new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC);
    DateTime instanceRequestDateRequestExpirationDate = instanceRequestDate.plusDays(30);

    LocalDate requestDate = new LocalDate(2017, 7, 22);
    LocalDate requestExpirationDate2 = requestDate.minusDays(30);

    IndividualResource instance = instancesFixture.basedUponDunkirk();
    IndividualResource holdings = holdingsFixture.defaultWithHoldings(instance.getId());

    //create 2 copies with no location id's assigned.
    final IndividualResource item1 = itemsFixture.basedUponDunkirkWithCustomHoldingAndLocation(holdings.getId(), null);
    final IndividualResource item2 = itemsFixture.basedUponDunkirkWithCustomHoldingAndLocationAndCheckedOut(holdings.getId(), null);

    //Set up request queues. Item1 has requests (1 queued request), Item2 is requests (1 queued), either could be satisfied.
    IndividualResource instanceRequester = usersFixture.jessica();

    requestsFixture.place(new RequestBuilder()
      .page()
      .forItem(item1)
      .withPickupServicePointId(pickupServicePointId)
      .withRequestExpiration(requestDate.plusDays(30))
      .by(instanceRequester));

    List<IndividualResource> patrons2 = new ArrayList<>();
    patrons2.add(usersFixture.steve());
    patrons2.add(usersFixture.rebecca());
    placeHoldRequest(item2, pickupServicePointId, patrons2, requestExpirationDate2);

    JsonObject requestBody = createInstanceRequestObject(instance.getId(), instanceRequester.getId(),
      pickupServicePointId, instanceRequestDate, instanceRequestDateRequestExpirationDate);

    CompletableFuture<Response> postCompleted = new CompletableFuture<>();

    client.post(InterfaceUrls.requestsUrl("/instances"), requestBody,
      ResponseHandler.any(postCompleted));

    Response postResponse = postCompleted.get(10, TimeUnit.SECONDS);
    assertEquals(201, postResponse.getStatusCode());

    JsonObject representation = postResponse.getJson();
    //Item2 should have been chosen because Jessica already requested item1
    validateInstanceRequestResponse(representation, pickupServicePointId, instance.getId(), item2.getId(), RequestType.HOLD);
  }

  private void validateInstanceRequestResponse(JsonObject representation,
                                               UUID pickupServicePointId,
                                               UUID instanceId,
                                               UUID itemId,
                                               RequestType expectedRequestType){
    assertNotNull(representation);
    assertEquals(pickupServicePointId.toString(), representation.getString("pickupServicePointId"));
    assertEquals("Circ Desk 1", representation.getJsonObject("pickupServicePoint").getString("name"));
    assertEquals(instanceId.toString(), representation.getJsonObject("item").getString("instanceId"));
    assertEquals(expectedRequestType.name(), representation.getString("requestType"));
    if (itemId != null)
      assertEquals(itemId.toString(), representation.getString("itemId"));
  }

  private JsonObject createInstanceRequestObject(UUID instanceId, UUID requesterId, UUID pickupServicePointId,
                                                 DateTime requestDate, DateTime requestExpirationDate){
    JsonObject requestBody = new JsonObject();
    requestBody.put("instanceId", instanceId.toString());
    requestBody.put("requestDate", requestDate.toString(ISODateTimeFormat.dateTime()));
    requestBody.put("requesterId", requesterId.toString());
    requestBody.put("pickupServicePointId", pickupServicePointId.toString());
    requestBody.put("fulfilmentPreference", "Hold Shelf");
    requestBody.put("requestExpirationDate", requestExpirationDate.toString(ISODateTimeFormat.dateTime()));

    return requestBody;
  }

  private void placeHoldRequest(IndividualResource item, UUID requestPickupServicePointId,
                                List<IndividualResource> patrons, LocalDate requestExpirationDate)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    for (IndividualResource patron : patrons) {
      final IndividualResource request = requestsFixture.place(new RequestBuilder()
        .hold()
        .forItem(item)
        .withPickupServicePointId(requestPickupServicePointId)
        .withRequestExpiration(requestExpirationDate)
        .by(patron));

      final String itemStatus = getNestedStringProperty(request.getJson(), "item", "status");

      assertThat(itemStatus, is("Checked out"));
    }
  }
}
