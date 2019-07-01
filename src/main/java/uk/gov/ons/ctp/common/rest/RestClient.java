package uk.gov.ons.ctp.common.rest;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A convenience class that wraps the Spring RestTemplate and eases its use around the typing,
 * headers, path and query params
 */
@Slf4j
public class RestClient {

  private RestClientConfig config;

  private RestTemplate restTemplate;

  @JacksonInject private ObjectMapper objectMapper;

  private Map<HttpStatus, HttpStatus> httpErrorMapping;
  private HttpStatus httpDefaultStatus;

  private static Map<HttpStatus, HttpStatus> defaultBareBonesErrorMapping;

  static {
    defaultBareBonesErrorMapping = new HashMap<HttpStatus, HttpStatus>();
    defaultBareBonesErrorMapping.put(HttpStatus.OK, HttpStatus.OK);
    defaultBareBonesErrorMapping.put(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND);
  }

  /** Construct with no details of the server - will use the default RestClientConfig provides */
  public RestClient() {
    this(new RestClientConfig());
  }

  /**
   * Constructor which uses no error code mappings.
   *
   * @param clientConfig contains data on how to connect to another service.
   */
  public RestClient(RestClientConfig clientConfig) {
    this(clientConfig, defaultBareBonesErrorMapping, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Construct with the core details of the server
   *
   * @param clientConfig contains data on how to connect to another service.
   * @param httpErrorMapping is a table which determines which error code this service should
   *     respond with following an http error from the delegated service. If a http request fails
   *     with a status code that is not in the table then the status code is used without any
   *     translation.
   * @param httpDefaultStatus if the called service returns a http code which is not in the mapping
   *     table then this value will be used.
   */
  public RestClient(
      RestClientConfig clientConfig,
      Map<HttpStatus, HttpStatus> httpErrorMapping,
      HttpStatus httpDefaultStatus) {
    this.config = clientConfig;
    this.httpErrorMapping = httpErrorMapping;
    this.httpDefaultStatus = httpDefaultStatus;
    init();
  }

  public void init() {
    restTemplate = new RestTemplate(clientHttpRequestFactory(config));
    objectMapper = new ObjectMapper();
  }

  private ClientHttpRequestFactory clientHttpRequestFactory(RestClientConfig clientConfig) {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    // set the timeout when establishing a connection
    // factory.setConnectTimeout(clientConfig.getConnectTimeoutMilliSeconds());
    // set the timeout when reading the response from a request
    // factory.setReadTimeout(clientConfig.getReadTimeoutMilliSeconds());
    return factory;
  }

  /**
   * Allow access to the underlying template
   *
   * @return the underlying template
   */
  public RestTemplate getRestTemplate() {
    return this.restTemplate;
  }

  /**
   * Use to perform a GET that retrieves a single resource
   *
   * @param <T> the type that will returned by the server we call
   * @param path the API path - can contain path params place holders in "{}" ie "/cases/{caseid}"
   * @param clazz the class type of the resource to be obtained
   * @param pathParams vargs list of params to substitute in the path - note simply used in order
   * @return the type you asked for! or null
   * @throws RestClientException something went wrong making http call
   */
  public <T> T getResource(String path, Class<T> clazz, Object... pathParams)
      throws RestClientException {
    return getResource(path, clazz, null, null, pathParams);
  }

  /**
   * Use to perform a GET that retrieves a single resource
   *
   * @param <T> the type that will returned by the server we call
   * @param path the API path - can contain path params place holders in "{}" ie "/cases/{caseid}"
   * @param clazz the class type of the resource to be obtained
   * @param headerParams map of header of params to be used - can be null
   * @param queryParams multi map of query params keyed by string logically allows for
   *     K:"haircolor",V:"blond" AND K:"shoesize", V:"9","10"
   * @param pathParams vargs list of params to substitute in the path - note simply used in order
   * @return the type you asked for! or null
   * @throws ResponseStatusException something went wrong making http call
   */
  public <T> T getResource(
      String path,
      Class<T> clazz,
      Map<String, String> headerParams,
      MultiValueMap<String, String> queryParams,
      Object... pathParams)
      throws ResponseStatusException {
    log.debug("Enter getResources for path: {}", path);

    // Issue GET request to other service
    HttpEntity<?> httpEntity = createHttpEntity(null, headerParams);
    UriComponents uriComponents = createUriComponents(path, queryParams, pathParams);
    ResponseEntity<String> response;
    try {
      response =
          restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, httpEntity, String.class);
    } catch (HttpStatusCodeException e) {
      // Failure detected. For 4xx and 5xx status codes
      log.error(
          "GET failed for path: '"
              + uriComponents
              + "' Status: "
              + e.getStatusCode()
              + " ResponseBody: '"
              + e.getResponseBodyAsString()
              + "'",
          e);
      throw new ResponseStatusException(
          mapToExternalStatus(e.getStatusCode()), "Internal processing error");
    } catch (RestClientException e) {
      log.error("GET failed for path: '" + uriComponents + "'", e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Internal processing error");
    }

    // Convert the response string to a DTO object
    T responseObject = null;
    String responseBody = response.getBody();
    if (responseBody == null) {
      String errorMessage =
          "Empty body returned for path '"
              + uriComponents.toUriString()
              + "'. Status code: "
              + response.getStatusCode();
      log.error(errorMessage);
      throw new ResponseStatusException(
          mapToExternalStatus(response.getStatusCode()), "Internal processing error");
    }
    try {
      responseObject = objectMapper.readValue(responseBody, clazz);
    } catch (IOException e) {
      log.error(
          "Failed to convert response to DTO object. Path: '"
              + uriComponents
              + "' Status: "
              + response.getStatusCodeValue()
              + " ResponseBody: '"
              + responseBody
              + "'",
          e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Internal processing error");
    }

    return responseObject;
  }

  /**
   * Use to perform a GET that retrieves multiple instances of a resource
   *
   * @param <T> the type that will returned by the server we call
   * @param path the API path - can contain path params place holders in "{}" ie "/cases/{caseid}"
   * @param clazz the class type of the resource, a List of which is to be obtained
   * @param pathParams vargs list of params to substitute in the path - note simply used in order
   * @return a list of the type you asked for
   * @throws RestClientException something went wrong making http call
   */
  public <T> List<T> getResources(String path, Class<T[]> clazz, Object... pathParams)
      throws RestClientException {
    return getResources(path, clazz, null, null, pathParams);
  }

  /**
   * Use to perform a GET that retrieves multiple instances of a resource
   *
   * @param <T> the type that will returned by the server we call
   * @param path the API path - can contain path params place holders in "{}" ie "/cases/{caseid}"
   * @param clazz the array class type of the resource, a List of which is to be obtained
   * @param headerParams map of header of params to be used - can be null
   * @param queryParams multi map of query params keyed by string logically allows for
   *     K:"haircolor",V:"blond" AND K:"shoesize", V:"9","10"
   * @param pathParams vargs list of params to substitute in the path - note simply used in order
   * @return a list of the type you asked for
   * @throws RestClientException something went wrong making http call
   */
  public <T> List<T> getResources(
      String path,
      Class<T[]> clazz,
      Map<String, String> headerParams,
      MultiValueMap<String, String> queryParams,
      Object... pathParams)
      throws RestClientException {

    log.debug("Enter getResources for path : {}", path);

    T[] responseArray = getResource(path, clazz, headerParams, queryParams, pathParams);

    List<T> responseList = new ArrayList<T>();
    if (responseArray.length > 0) {
      responseList = Arrays.asList(responseArray);
    }

    return responseList;
  }

  /**
   * used to post
   *
   * @param <T> the type that will returned by the server we call
   * @param <O> the type to be sent
   * @param path the url path
   * @param objToPost the object to be sent
   * @param clazz the expected response object type
   * @param pathParams var arg path params in {} placeholder order
   * @return the response object
   * @throws RestClientException something went wrong calling the server
   */
  public <T, O> T postResource(String path, O objToPost, Class<T> clazz, Object... pathParams)
      throws RestClientException {
    return postResource(path, objToPost, clazz, null, null, pathParams);
  }

  /**
   * used to post
   *
   * @param <T> the type that will returned by the server we call
   * @param <O> the type to be sent
   * @param path the url path
   * @param objToPost the object to be sent
   * @param clazz the expected response object type
   * @param headerParams map of header params
   * @param queryParams multi map of query params
   * @param pathParams var arg path params in {} placeholder order
   * @return the response object
   * @throws RestClientException something went wrong calling the server
   */
  public <T, O> T postResource(
      String path,
      O objToPost,
      Class<T> clazz,
      Map<String, String> headerParams,
      MultiValueMap<String, String> queryParams,
      Object... pathParams)
      throws RestClientException {
    return executePutOrPost(
        HttpMethod.POST, path, objToPost, clazz, headerParams, queryParams, pathParams);
  }

  /**
   * used to put
   *
   * @param <T> the type that will returned by the server we call
   * @param <O> the type to be sent
   * @param path the url path
   * @param objToPut the object to be sent
   * @param clazz the expected response object type
   * @param pathParams var arg path params in {} placeholder order
   * @return the response object
   * @throws RestClientException something went wrong calling the server
   */
  public <T, O> T putResource(String path, O objToPut, Class<T> clazz, Object... pathParams)
      throws RestClientException {
    return putResource(path, objToPut, clazz, null, null, pathParams);
  }

  /**
   * used to put
   *
   * @param <T> the type that will returned by the server we call
   * @param <O> the type to be sent
   * @param path the url path
   * @param objToPut the object to be sent
   * @param clazz the expected response object type
   * @param headerParams map of header params
   * @param queryParams multi map of query params
   * @param pathParams var arg path params in {} placeholder order
   * @return the response object
   * @throws RestClientException something went wrong calling the server
   */
  public <T, O> T putResource(
      String path,
      O objToPut,
      Class<T> clazz,
      Map<String, String> headerParams,
      MultiValueMap<String, String> queryParams,
      Object... pathParams)
      throws RestClientException {
    return executePutOrPost(
        HttpMethod.PUT, path, objToPut, clazz, headerParams, queryParams, pathParams);
  }

  /**
   * used to put or post
   *
   * @param <T> the type that will returned by the server we call
   * @param <O> the type to be sent
   * @param method put or post
   * @param path the url path
   * @param objToPut the object to be sent
   * @param clazz the expected response object type
   * @param headerParams map of header params
   * @param queryParams multi map of query params
   * @param pathParams var arg path params in {} placeholder order
   * @return the response object
   * @throws RestClientException something went wrong calling the server
   */
  private <T, O> T executePutOrPost(
      HttpMethod method,
      String path,
      O objToPut,
      Class<T> clazz,
      Map<String, String> headerParams,
      MultiValueMap<String, String> queryParams,
      Object... pathParams)
      throws RestClientException {
    log.debug("Enter executePutOrPost for path : {}", path);

    HttpEntity<O> httpEntity = createHttpEntity(objToPut, headerParams);
    UriComponents uriComponents = createUriComponents(path, queryParams, pathParams);

    ResponseEntity<T> response =
        restTemplate.exchange(uriComponents.toUri(), method, httpEntity, clazz);

    if (!response.getStatusCode().is2xxSuccessful()) {
      log.error("Failed to put/post when calling {}", uriComponents.toUri());
      throw new RestClientException(
          "Unexpected response status" + response.getStatusCode().value());
    }
    return response.getBody();
  }

  /**
   * used to create the URiComponents needed to call an endpoint
   *
   * @param path the API path - can contain path params place holders in "{}" ie "/cases/{caseid}"
   * @param queryParams multi map of query params keyed by string logically allows for
   *     K:"haircolor",V:"blond" AND K:"shoesize", V:"9","10"
   * @param pathParams vargs list of params to substitute in the path - note simply used in order
   * @return the components
   */
  private UriComponents createUriComponents(
      String path, MultiValueMap<String, String> queryParams, Object... pathParams) {
    UriComponents uriComponents =
        UriComponentsBuilder.newInstance()
            .scheme(config.getScheme())
            .host(config.getHost())
            .port(config.getPort())
            .path(path)
            .queryParams(queryParams)
            .buildAndExpand(pathParams)
            .encode();
    return uriComponents;
  }

  /**
   * used to create the HttpEntity for headers
   *
   * @param <H> the type wrapped by the entity
   * @param entity the object to be wrapped in the entity
   * @param headerParams map of header of params to be used - can be null
   * @return the header entity
   */
  private <H> HttpEntity<H> createHttpEntity(H entity, Map<String, String> headerParams) {
    HttpHeaders headers = new HttpHeaders();

    headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    if (headerParams != null) {
      for (Map.Entry<String, String> me : headerParams.entrySet()) {
        headers.set(me.getKey(), me.getValue());
      }
    }

    if (this.config.getUsername() != null && this.config.getPassword() != null) {
      String auth = this.config.getUsername() + ":" + this.config.getPassword();
      byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII")));
      String authHeader = "Basic " + new String(encodedAuth);
      headers.set("Authorization", authHeader);
    }
    HttpEntity<H> httpEntity = new HttpEntity<H>(entity, headers);
    return httpEntity;
  }

  /**
   * This method converts a http code from a failed invocation of another service to a http status
   * that this service should fail with.
   *
   * @param originalHttpStatus is the status returned by the other service.
   * @return the status that this service should fail with.
   */
  private HttpStatus mapToExternalStatus(HttpStatus originalHttpStatus) {
    if (httpErrorMapping.containsKey(originalHttpStatus)) {
      return httpErrorMapping.get(originalHttpStatus);
    }

    return httpDefaultStatus;
  }
}
