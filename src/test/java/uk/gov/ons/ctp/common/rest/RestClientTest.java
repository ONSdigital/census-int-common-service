package uk.gov.ons.ctp.common.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Test the RestClient class */
public class RestClientTest {

  /** A test */
  @Test
  public void testPutResourceOk() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels/42"))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withSuccess());

    FakeDTO fakeDTO = new FakeDTO("blue", 52);
    restClient.putResource("/hotels/{hotelId}", fakeDTO, FakeDTO.class, "42");
    mockServer.verify();
  }

  /*
   * A test
   */
  @Test
  public void testPostResourceOk() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels/42"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    FakeDTO fakeDTO = new FakeDTO("blue", 52);
    restClient.postResource("/hotels/{hotelId}", fakeDTO, FakeDTO.class, "42");
    mockServer.verify();
  }

  /** Test that we get a failure when we ask for a connection to a non resolvable host */
  @Test(expected = ResponseStatusException.class)
  public void testGetTimeoutURLInvalid() {
    RestClientConfig config =
        RestClientConfig.builder()
            .scheme("http")
            .host("phil.whiles.for.president.com")
            .port("80")
            .build();
    RestClient restClient = new RestClient(config);
    restClient.getResource("/hairColor/blue/shoeSize/10", FakeDTO.class);
  }

  /** A test */
  @Test
  public void testGetResourceOk() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels/42"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{ \"hairColor\" : \"blonde\", \"shoeSize\" : \"8\"}", MediaType.APPLICATION_JSON));

    FakeDTO fakeDTO = restClient.getResource("/hotels/{hotelId}", FakeDTO.class, "42");
    assertTrue(fakeDTO != null);
    assertTrue(fakeDTO.getHairColor().equals("blonde"));
    assertTrue(fakeDTO.getShoeSize().equals(8));
    mockServer.verify();
  }

  /** A test */
  @Test
  public void testGetResourceFailsWithInvalidJsonResponse() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels/42"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess("{ \"invalid-json\" ! payload %^#%%%! }", MediaType.APPLICATION_JSON));

    try {
      restClient.getResource("/hotels/{hotelId}", FakeDTO.class, "42");
      fail();
    } catch (ResponseStatusException e) {
      mockServer.verify();
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatus());
    }
  }

  /**
   * A test in which the fake service responds with an error status, but that status is not remapped
   * as there is no matching entry in the mapping table.
   */
  @Test
  public void testGetResourceFailsWithUnmappedError() {
    HttpStatus defaultHttpStatus = HttpStatus.CHECKPOINT;

    // Setup the client to error mapping that should not be used
    RestClientConfig config =
        RestClientConfig.builder().scheme("http").host("localhost").port("8080").build();
    Map<HttpStatus, HttpStatus> errorMappings =
        Map.of(HttpStatus.HTTP_VERSION_NOT_SUPPORTED, HttpStatus.I_AM_A_TEAPOT);
    RestClient restClient = new RestClient(config, errorMappings, defaultHttpStatus);

    // Get the mocked rest template to fail with an error that is not in the error mapping
    RestTemplate restTemplate = restClient.getRestTemplate();
    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels/42"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.CONFLICT));

    // Invoke the rest client and confirm that the error used is the supplied default
    try {
      restClient.getResource("/hotels/{hotelId}", FakeDTO.class, "42");
      fail();
    } catch (ResponseStatusException e) {
      mockServer.verify();
      assertEquals(defaultHttpStatus, e.getStatus());
    }
  }

  /**
   * A test in which the fake service fails, as it does in the previous test, but this time the
   * RestClient should match an error mapping rule
   */
  @Test
  public void testGetResourceFailsWithMappedError() {
    RestClientConfig config =
        RestClientConfig.builder().scheme("http").host("localhost").port("8080").build();
    Map<HttpStatus, HttpStatus> errorMappings =
        Map.of(HttpStatus.CONFLICT, HttpStatus.I_AM_A_TEAPOT);
    RestClient restClient = new RestClient(config, errorMappings, HttpStatus.INTERNAL_SERVER_ERROR);

    // Setup the mocked rest template to fail with an error that will me mapped
    RestTemplate restTemplate = restClient.getRestTemplate();
    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels/42"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.CONFLICT));

    // Invoke the rest client and confirm it has failed with a remapped error
    try {
      restClient.getResource("/hotels/{hotelId}", FakeDTO.class, "42");
      fail();
    } catch (ResponseStatusException e) {
      mockServer.verify();
      assertEquals(HttpStatus.I_AM_A_TEAPOT, e.getStatus());
    }
  }

  /** A test */
  @Test
  public void testGetResourceNotFound() {
    RestClientConfig config =
        RestClientConfig.builder().scheme("http").host("localhost").port("8080").build();
    RestClient restClient = new RestClient(config);

    RestTemplate restTemplate = restClient.getRestTemplate();
    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels/42"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withStatus(HttpStatus.NOT_FOUND)
                .body(
                    "{ \"error\" :{  \"code\" : \"123\", \"message\" : \"123\", \"timestamp\" : \"123\"}}"));

    try {
      restClient.getResource("/hotels/{hotelId}", FakeDTO.class, "42");
      fail();
    } catch (ResponseStatusException e) {
      mockServer.verify();
      assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }
  }

  /** A test */
  @Test
  public void testGetResourcesOk() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "[{ \"hairColor\" : \"blonde\", \"shoeSize\" : \"8\"},{ \"hairColor\" : \"brown\", \"shoeSize\" : \"12\"}]",
                MediaType.APPLICATION_JSON));

    List<FakeDTO> fakeDTOs = restClient.getResources("/hotels", FakeDTO[].class);
    assertTrue(fakeDTOs != null);
    assertTrue(fakeDTOs.size() == 2);
    mockServer.verify();
  }

  /** A test */
  @Test(expected = ResponseStatusException.class)
  public void testGetResourcesNoContent() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));

    restClient.getResources("/hotels", FakeDTO[].class);
  }

  /** A test */
  @Test(expected = ResponseStatusException.class)
  public void testGetResourcesReallyNotOk() {
    RestClientConfig config =
        RestClientConfig.builder().scheme("http").host("localhost").port("8080").build();
    RestClient restClient = new RestClient(config);
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    restClient.getResources("/hotels", FakeDTO[].class);
  }

  /** A test */
  @Test(expected = ResponseStatusException.class)
  public void testGetResourcesUnauthorized() {
    RestClientConfig config =
        RestClientConfig.builder().scheme("http").host("localhost").port("8080").build();
    RestClient restClient = new RestClient(config);
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    restClient.getResources("/hotels", FakeDTO[].class);
  }
}
