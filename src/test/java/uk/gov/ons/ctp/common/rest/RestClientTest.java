package uk.gov.ons.ctp.common.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.error.CTPException;

/** Test the RestClient class */
public class RestClientTest {

  /**
   * A test
   *
   * @throws CTPException
   */
  @Test
  public void testPutResourceOk() throws CTPException {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels/42"))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(
            withSuccess(
                "{ \"hairColor\" : \"blonde\", \"shoeSize\" : \"8\"},{ \"hairColor\" : \"brown\", \"shoeSize\" : \"12\"}",
                MediaType.APPLICATION_JSON));

    FakeDTO fakeDTO = new FakeDTO("blue", 52);
    restClient.putResource("/hotels/{hotelId}", fakeDTO, FakeDTO.class, "42");
    mockServer.verify();
  }

  /*
   * A test
   * @throws CTPException
   */
  @Test
  public void testPostResourceOk() throws CTPException {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels/42"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{ \"hairColor\" : \"blonde\", \"shoeSize\" : \"8\"},{ \"hairColor\" : \"brown\", \"shoeSize\" : \"12\"}",
                MediaType.APPLICATION_JSON));

    FakeDTO fakeDTO = new FakeDTO("blue", 52);
    restClient.postResource("/hotels/{hotelId}", fakeDTO, FakeDTO.class, "42");
    mockServer.verify();
  }

  @Test
  public void testPostResource_StringResponse() throws CTPException {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels/42"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("ABC123", MediaType.TEXT_PLAIN));

    FakeDTO fakeDTO = new FakeDTO("blue", 52);
    String response = restClient.postResource("/hotels/{hotelId}", fakeDTO, String.class, "42");
    mockServer.verify();
    assertEquals("ABC123", response);
  }

  @Test
  public void testPostResource_NullResponse() throws CTPException {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels/42"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withNoContent());

    Exception exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              FakeDTO fakeDTO = new FakeDTO("blue", 52);
              restClient.postResource("/hotels/{hotelId}", fakeDTO, String.class, "42");
            });

    mockServer.verify();
    assertTrue(exception.getMessage(), exception.getMessage().contains("No response"));
  }

  /**
   * Test that we get a failure when we ask for a connection to a non resolvable host
   *
   * @throws CTPException
   */
  @Test(expected = ResponseStatusException.class)
  public void testGetTimeoutURLInvalid() throws CTPException {
    RestClientConfig config =
        RestClientConfig.builder()
            .scheme("http")
            .host("phil.whiles.for.president.com")
            .port("80")
            .connectionManagerDefaultMaxPerRoute(4)
            .connectionManagerMaxTotal(11)
            .build();
    RestClient restClient = new RestClient(config);
    restClient.getResource("/hairColor/blue/shoeSize/10", FakeDTO.class);
  }

  /**
   * A test
   *
   * @throws CTPException
   */
  @Test
  public void testGetResourceOk() throws CTPException {
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

  /**
   * A test
   *
   * @throws CTPException
   */
  @Test
  public void testGetResourceFailsWithInvalidJsonResponse() throws CTPException {
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
      assertNull(e.getCause());
    }
  }

  /**
   * A test in which the fake service responds with an error status, but that status is not remapped
   * as there is no matching entry in the mapping table.
   *
   * @throws CTPException
   */
  @Test
  public void testGetResourceFailsWithUnmappedError() throws CTPException {
    HttpStatus defaultHttpStatus = HttpStatus.CHECKPOINT;

    // Setup the client to error mapping that should not be used
    RestClientConfig config =
        RestClientConfig.builder()
            .scheme("http")
            .host("localhost")
            .port("8080")
            .connectionManagerDefaultMaxPerRoute(4)
            .connectionManagerMaxTotal(11)
            .build();
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
      assertEquals(HttpStatus.CONFLICT, ((HttpStatusCodeException) e.getCause()).getStatusCode());
    }
  }

  /**
   * A test in which the fake service fails, as it does in the previous test, but this time the
   * RestClient should match an error mapping rule
   *
   * @throws CTPException
   */
  @Test
  public void testGetResourceFailsWithMappedError() throws CTPException {
    RestClientConfig config =
        RestClientConfig.builder()
            .scheme("http")
            .host("localhost")
            .port("8080")
            .connectionManagerDefaultMaxPerRoute(4)
            .connectionManagerMaxTotal(11)
            .build();
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
      assertEquals(HttpStatus.CONFLICT, ((HttpStatusCodeException) e.getCause()).getStatusCode());
    }
  }

  /**
   * A test
   *
   * @throws CTPException
   */
  @Test
  public void testGetResourcesOk() throws CTPException {
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

  /**
   * A test
   *
   * @throws CTPException
   */
  @Test
  public void testGetResourceNotFound() throws CTPException {
    mockRequest(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND, true);
  }

  /**
   * A test
   *
   * @throws CTPException
   */
  public void testGetResourcesNoContent() throws CTPException {
    mockRequest(HttpStatus.NO_CONTENT, HttpStatus.INTERNAL_SERVER_ERROR, false);
  }

  /**
   * A test
   *
   * @throws CTPException
   */
  public void testGetResourcesReallyNotOk() throws CTPException {
    mockRequest(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR, true);
  }

  /**
   * A test
   *
   * @throws CTPException
   */
  @Test
  public void testGetResourcesUnauthorized() throws CTPException {
    mockRequest(HttpStatus.UNAUTHORIZED, HttpStatus.INTERNAL_SERVER_ERROR, true);
  }

  private void mockRequest(HttpStatus responseStatus, HttpStatus mapStatus, boolean cause)
      throws CTPException {
    RestClientConfig config =
        RestClientConfig.builder()
            .scheme("http")
            .host("localhost")
            .port("8080")
            .connectionManagerDefaultMaxPerRoute(4)
            .connectionManagerMaxTotal(11)
            .build();
    RestClient restClient = new RestClient(config);
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(responseStatus));

    try {
      restClient.getResources("/hotels", FakeDTO[].class);
      fail();
    } catch (ResponseStatusException e) {
      mockServer.verify();
      assertEquals(mapStatus, e.getStatus());
      if (cause) {
        assertEquals(responseStatus, ((HttpStatusCodeException) e.getCause()).getStatusCode());
      } else {
        assertNull(e.getCause());
      }
    }
  }
}
