package uk.gov.ons.ctp.common.rest;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.UnknownHostException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Test the RestClient class */
public class RestClientTest {

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

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

  /**
   * Test that we get an underlying UnknownHostException when we ask for a connection to a non
   * resolvable host
   *
   * @throws Throwable
   */
  @Test(expected = UnknownHostException.class)
  public void testGetTimeoutURLInvalid() throws Throwable {
    RestClientConfig config =
        RestClientConfig.builder()
            .scheme("http")
            .host("phil.whiles.for.president.com")
            .port("80")
            .build();
    RestClient restClient = new RestClient(config);
    try {
      restClient.getResource("/hairColor/blue/shoeSize/10", FakeDTO.class);
    } catch (ResourceAccessException rae) {
      throw rae.getCause();
    }
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
  @Test(expected = RestClientException.class)
  public void testGetResourceReallyNotOk() {
    RestClientConfig config =
        RestClientConfig.builder().scheme("http").host("localhost").port("8080").build();
    RestClient restClient = new RestClient(config);
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);

    for (int i = 0; i < 3; i++) {
      mockServer
          .expect(requestTo("http://localhost:8080/hotels/42"))
          .andExpect(method(HttpMethod.GET))
          .andRespond(withStatus(HttpStatus.CONFLICT));
    }

    restClient.getResource("/hotels/{hotelId}", FakeDTO.class, "42");
  }

  /** A test */
  @Test(expected = RestClientException.class)
  public void testGetResourceNotFound() {
    RestClientConfig config =
        RestClientConfig.builder().scheme("http").host("localhost").port("8080").build();
    RestClient restClient = new RestClient(config);
    ;
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    for (int i = 0; i < 3; i++) {
      mockServer
          .expect(requestTo("http://localhost:8080/hotels/42"))
          .andExpect(method(HttpMethod.GET))
          .andRespond(
              withStatus(HttpStatus.NOT_FOUND)
                  .body(
                      "{ \"error\" :{  \"code\" : \"123\", \"message\" : \"123\", \"timestamp\" : \"123\"}}"));
    }

    FakeDTO fakeDTO = restClient.getResource("/hotels/{hotelId}", FakeDTO.class, "42");
    assertTrue(fakeDTO == null);
    mockServer.verify();
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
  @Test
  public void testGetResourcesNoContent() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer
        .expect(requestTo("http://localhost:8080/hotels"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));

    List<FakeDTO> fakeDTOs = restClient.getResources("/hotels", FakeDTO[].class);
    assertTrue(fakeDTOs != null);
    assertTrue(fakeDTOs.size() == 0);
    mockServer.verify();
  }

  /** A test */
  @Test(expected = RestClientException.class)
  public void testGetResourcesReallyNotOk() {
    RestClientConfig config =
        RestClientConfig.builder().scheme("http").host("localhost").port("8080").build();
    RestClient restClient = new RestClient(config);
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    for (int i = 0; i < 3; i++) {
      mockServer
          .expect(requestTo("http://localhost:8080/hotels"))
          .andExpect(method(HttpMethod.GET))
          .andRespond(withStatus(HttpStatus.BAD_REQUEST));
    }

    restClient.getResources("/hotels", FakeDTO[].class);
  }

  /** A test */
  @Test(expected = RestClientException.class)
  public void testGetResourceUnauthorized() {
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
