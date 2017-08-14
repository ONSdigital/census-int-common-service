package uk.gov.ons.ctp.common.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;

/**
 * Test the RestClient class
 */
public class RestClientTest {

  /**
   * Set-up unit test
   */
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  /**
   * Test Put
   */
  @Test
  public void testPutResourceOk() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer.expect(requestTo("http://localhost:8080/hotels/42")).andExpect(new RequestMatcher() {
      @Override
      public void match(ClientHttpRequest request) throws IOException, AssertionError {
        MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).match(request);
        MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).match(request);
        MockRestRequestMatchers.method(HttpMethod.PUT).match(request);
      }
    }).andRespond(withSuccess());

    FakeDTO fakeDTO = new FakeDTO("blue", 52);
    restClient.putResource("/hotels/{hotelId}", fakeDTO, FakeDTO.class, "42");
    mockServer.verify();
  }

  /**
   * Post without headers
   */
  @Test
  public void testPostResourceOk() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer.expect(requestTo("http://localhost:8080/hotels/42")).andExpect(new RequestMatcher() {
      @Override
      public void match(ClientHttpRequest request) throws IOException, AssertionError {
        MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).match(request);
        MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).match(request);
        MockRestRequestMatchers.method(HttpMethod.POST).match(request);
      }
    }).andRespond(withSuccess());

    FakeDTO fakeDTO = new FakeDTO("blue", 52);
    restClient.postResource("/hotels/{hotelId}", fakeDTO, FakeDTO.class, "42");
    mockServer.verify();
  }

  /**
   * Post with headers
   */
  @Test
  public void testPostResourceHeadersOk() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();
    Map<String, String> headerParams = new HashMap<String, String>();
    headerParams.put("Test1", "foo");
    headerParams.put("Test2", "bar");

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer.expect(requestTo("http://localhost:8080/hotels/42")).andExpect(new RequestMatcher() {
      @Override
      public void match(ClientHttpRequest request) throws IOException, AssertionError {
        MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).match(request);
        MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).match(request);
        MockRestRequestMatchers.header("Test1", "foo").match(request);
        MockRestRequestMatchers.header("Test2", "bar").match(request);
        MockRestRequestMatchers.method(HttpMethod.POST).match(request);
      }
    }).andRespond(withSuccess());

    FakeDTO fakeDTO = new FakeDTO("blue", 52);
    restClient.postResource("/hotels/{hotelId}", fakeDTO, FakeDTO.class, headerParams, null, "42");
    mockServer.verify();
  }

  /**
   * Test that we can call the json echo service with a very short timeout ie we
   * expect to get a timeout! this is proving that our RestClient can be
   * configured with a timeout for circuit breaker use
   */
  @Test
  public void testGetTimeoutFail() {
    RestClientConfig config = RestClientConfig.builder()
        .scheme("http")
        .host("jsontest.com")
        .port("80")
        .retryAttempts(3)
        .retryPauseMilliSeconds(2)
        .connectTimeoutMilliSeconds(1)
        .readTimeoutMilliSeconds(1)
        .build();
    RestClient restClient = new RestClient(config);
    try {
      restClient.getResource("/maryhadalittlehorse", FakeDTO.class);
      fail();
    } catch (RestClientException e) {
      assertTrue(e.getMessage().contains(
          "cause = org.apache.http.conn.ConnectTimeoutException"));
    }
  }

  /**
   * Test that we get an underlying UnknownHostException when we ask for a
   * connection to a non resolvable host
   */
  @Test
  public void testGetTimeoutURLInvalid() {
    RestClientConfig config = RestClientConfig.builder()
        .scheme("http")
        .host("phil.whiles.for.president.com")
        .port("80")
        .retryAttempts(1)
        .retryPauseMilliSeconds(100)
        .connectTimeoutMilliSeconds(1000)
        .readTimeoutMilliSeconds(1000)
        .build();
    RestClient restClient = new RestClient(config);
    try {
      restClient.getResource("/hairColor/blue/shoeSize/10", FakeDTO.class);
      fail();
    } catch (RestClientException e) {
      assertTrue(e.getMessage().contains(
          "cause = java.net.UnknownHostException"));
    }
  }

  /**
   * Test that we can call the json echo service with a very long timeout
   */
  @Test
  public void testGetTimeoutOK() {
    RestClientConfig config = RestClientConfig.builder()
        .scheme("http")
        .host("echo.jsontest.com")
        .port("80")
        .retryAttempts(3)
        .retryPauseMilliSeconds(100)
        .connectTimeoutMilliSeconds(1000)
        .readTimeoutMilliSeconds(1000)
        .build();

    RestClient restClient = new RestClient(config);

    FakeDTO fakeDTOpage = restClient.getResource("/hairColor/blue/shoeSize/10", FakeDTO.class);
    assertTrue(fakeDTOpage != null);
  }

  /**
   * A test
   */
  @Test
  public void testGetResourceOk() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer.expect(requestTo("http://localhost:8080/hotels/42")).andExpect(new RequestMatcher() {
      @Override
      public void match(ClientHttpRequest request) throws IOException, AssertionError {
        MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).match(request);
        MockRestRequestMatchers.method(HttpMethod.GET).match(request);
      }
    }).andRespond(withSuccess("{ \"hairColor\" : \"blonde\", \"shoeSize\" : \"8\"}", MediaType.APPLICATION_JSON));

    FakeDTO fakeDTO = restClient.getResource("/hotels/{hotelId}", FakeDTO.class, "42");
    assertTrue(fakeDTO != null);
    assertTrue(fakeDTO.getHairColor().equals("blonde"));
    assertTrue(fakeDTO.getShoeSize().equals(8));
    mockServer.verify();
  }

  /**
   * A test
   */
  @Test(expected = RestClientException.class)
  public void testGetResourceReallyNotOk() {
    RestClientConfig config = RestClientConfig.builder()
        .scheme("http")
        .host("localhost")
        .port("8080")
        .retryAttempts(3)
        .retryPauseMilliSeconds(2)
        .connectTimeoutMilliSeconds(1)
        .readTimeoutMilliSeconds(1)
        .build();
    RestClient restClient = new RestClient(config);
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);

    for (int i = 0; i < 3; i++) {
      mockServer.expect(requestTo("http://localhost:8080/hotels/42")).andExpect(method(HttpMethod.GET))
          .andRespond(withStatus(HttpStatus.CONFLICT));
    }

    restClient.getResource("/hotels/{hotelId}", FakeDTO.class, "42");
  }

  /**
   * A test
   */
  @Test(expected = RestClientException.class)
  public void testGetResourceNotFound() {
    RestClientConfig config = RestClientConfig.builder()
        .scheme("http")
        .host("localhost")
        .port("8080")
        .retryAttempts(3)
        .retryPauseMilliSeconds(2)
        .connectTimeoutMilliSeconds(1)
        .readTimeoutMilliSeconds(1)
        .build();
    RestClient restClient = new RestClient(config);

    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    for (int i = 0; i < 3; i++) {
      mockServer.expect(requestTo("http://localhost:8080/hotels/42")).andExpect(method(HttpMethod.GET))
          .andRespond(withStatus(HttpStatus.NOT_FOUND)
              .body("{ \"error\" :{  \"code\" : \"123\", \"message\" : \"123\", \"timestamp\" : \"123\"}}"));
    }

    FakeDTO fakeDTO = restClient.getResource("/hotels/{hotelId}", FakeDTO.class, "42");
    assertTrue(fakeDTO == null);
    mockServer.verify();
  }

  /**
   * A test
   */
  @Test
  public void testGetResourcesOk() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer.expect(requestTo("http://localhost:8080/hotels")).andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
            "[{ \"hairColor\" : \"blonde\", \"shoeSize\" : \"8\"},{ \"hairColor\" : \"brown\", \"shoeSize\" : \"12\"}]",
            MediaType.APPLICATION_JSON));

    List<FakeDTO> fakeDTOs = restClient.getResources("/hotels", FakeDTO[].class);
    assertTrue(fakeDTOs != null);
    assertTrue(fakeDTOs.size() == 2);
    mockServer.verify();
  }

  /**
   * A test
   */
  @Test
  public void testGetResourcesNoContent() {
    RestClient restClient = new RestClient();
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer.expect(requestTo("http://localhost:8080/hotels")).andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));

    List<FakeDTO> fakeDTOs = restClient.getResources("/hotels", FakeDTO[].class);
    assertTrue(fakeDTOs != null);
    assertTrue(fakeDTOs.size() == 0);
    mockServer.verify();
  }

  /**
   * A test
   */
  @Test(expected = RestClientException.class)
  public void testGetResourcesReallyNotOk() {
    RestClientConfig config = RestClientConfig.builder()
        .scheme("http")
        .host("localhost")
        .port("8080")
        .retryAttempts(3)
        .retryPauseMilliSeconds(2)
        .connectTimeoutMilliSeconds(1)
        .readTimeoutMilliSeconds(1)
        .build();
    RestClient restClient = new RestClient(config);
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    for (int i = 0; i < 3; i++) {
      mockServer.expect(requestTo("http://localhost:8080/hotels")).andExpect(method(HttpMethod.GET))
          .andRespond(withStatus(HttpStatus.BAD_REQUEST));
    }

    restClient.getResources("/hotels", FakeDTO[].class);
  }

  /**
   * A test
   */
  @Test(expected = RestClientException.class)
  public void testGetResourceUnauthorized() {
    RestClientConfig config = RestClientConfig.builder()
        .scheme("http")
        .host("localhost")
        .port("8080")
        .retryAttempts(3)
        .retryPauseMilliSeconds(2)
        .connectTimeoutMilliSeconds(1)
        .readTimeoutMilliSeconds(1)
        .build();
    RestClient restClient = new RestClient(config);
    RestTemplate restTemplate = restClient.getRestTemplate();

    MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
    mockServer.expect(requestTo("http://localhost:8080/hotels")).andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    restClient.getResources("/hotels", FakeDTO[].class);
  }

  /**
   * Create UriComponents with a JSON string for a query parameter named
   * searchString and no path parameters.
   */
  @Test
  public void testCreateUriComponentsQueryParamJSONNoPathParam() {

    RestClient restClient = new RestClient();

    String path = "/collectioninstrument";
    Object[] pathParams = new Object[0];
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("searchString",
        "{\"RU_REF\":\"50000064647\",\"COLLECTION_EXERCISE\":\"14fb3e68-4dca-46db-bf49-04b84e07e77c\"}");

    UriComponents uriComponents = restClient.createUriComponents(path, queryParams, pathParams);
    assertEquals(
        "http://localhost:8080/collectioninstrument?searchString=%7B%22RU_REF%22:%2250000064647%22,"
            + "%22COLLECTION_EXERCISE%22:%2214fb3e68-4dca-46db-bf49-04b84e07e77c%22%7D",
        uriComponents.toString());
  }

  /**
   * Create UriComponents with path parameters and no query parameters.
   */

  @Test
  public void testCreateUriComponentsNoQueryParamsWithPathParams() {

    RestClient restClient = new RestClient();

    String path = "/surveys/{surveyId}/classifiertypeselectors/{selectorId}";
    Object[] pathParams = {"cb0711c3-0ac8-41d3-ae0e-567e5ea1ef87", "efa868fb-fb80-44c7-9f33-d6800a17c4da"};

    UriComponents uriComponents = restClient.createUriComponents(path, null, pathParams);
    assertEquals(
        "http://localhost:8080/surveys/cb0711c3-0ac8-41d3-ae0e-567e5ea1ef87/"
            + "classifiertypeselectors/efa868fb-fb80-44c7-9f33-d6800a17c4da",
        uriComponents.toString());
  }
}
