package uk.gov.ons.ctp.common.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.util.RetryCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A convenience class that wraps the Spring RestTemplate and eases its use
 * around the typing, headers, path and query params
 */
@Slf4j
public class RestClient {

  private RestClientConfig config;

  private RestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  /**
   * Our error handler asks this class what constitues an error
   *
   * @param status the http status
   * @return true if an error
   */
  public static boolean isError(HttpStatus status) {
    HttpStatus.Series series = status.series();
    return (HttpStatus.Series.CLIENT_ERROR.equals(series)
        || HttpStatus.Series.SERVER_ERROR.equals(series));
  }

  /**
   * Construct with no details of the server - will use the default
   * RestClientConfig provides
   */
  public RestClient() {
    super();
    this.config = new RestClientConfig();
    init();
  }

  /**
   * Construct with the core details of the server
   *
   * @param clientConfig the configuration
   */
  public RestClient(RestClientConfig clientConfig) {
    super();
    this.config = clientConfig;
    init();
  }

  /**
   * Initialise this RestClient instance
   */
  public void init() {
    restTemplate = new RestTemplate(clientHttpRequestFactory(config));
    objectMapper = new ObjectMapper();
  }

  /**
   * Create a HttpComponentsClientHttpRequestFactory to create Client HTPRequests for a specified URI and HTTPMethod
   * @param clientConfig configuration object for requests
   * @return factory to create client requests for a given URI and HTTPMethod
   */
  private ClientHttpRequestFactory clientHttpRequestFactory(RestClientConfig clientConfig) {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    // set the timeout when establishing a connection
    factory.setConnectTimeout(clientConfig.getConnectTimeoutMilliSeconds());
    // set the timeout when reading the response from a request
    factory.setReadTimeout(clientConfig.getReadTimeoutMilliSeconds());
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
   * The client when using the RetryCommand may elect to have errors inspected
   * by a handler. If the handler returns true the retryCommand will keep
   * re-trying. Retries server errors, HttpStatus codes 5XX and any connection
   * failures. All other errors such as apparent client errors, HttpStatus
   * codes 4XX will return false to stop retries.
   *
   * @return boolean whether to retry Restful request
   */
  public Predicate<Exception> shouldRetry() {
    return new Predicate<Exception>() {

      public boolean test(Exception ex) {
        boolean retry = false;
        if (HttpStatusCodeException.class.isAssignableFrom(ex.getClass())) {
          if (((HttpStatusCodeException) ex).getStatusCode().is5xxServerError()) {
            // Any 500 server error retry
            retry = true;
          }
        } else if (ex instanceof ResourceAccessException) {
          // Simply cannot connect to resource. RestTemplate throws
          // ResourceAccessException on any IOException so retry.
          retry = true;
        }
        return retry;
      }
    };
  }

  /**
   * Use to perform a GET that retrieves a single resource
   *
   * @param <T> the type that will returned by the server we call
   * @param path the API path - can contain path params place holders in "{}" ie
   *          "/cases/{caseid}"
   * @param clazz the class type of the resource to be obtained
   * @param pathParams vargs list of params to substitute in the path - note
   *          simply used in order
   * @return the type you asked for! or null
   * @throws RestClientException something went wrong making http call
   */
  public <T> T getResource(
      String path,
      Class<T> clazz,
      Object... pathParams)
      throws RestClientException {
    return getResource(path, clazz, null, null, pathParams);
  }

  /**
   * Use to perform a GET that retrieves a single resource
   *
   * @param <T> the type that will returned by the server we call
   * @param path the API path - can contain path params place holders in "{}" ie
   *          "/cases/{caseid}"
   * @param clazz the class type of the resource to be obtained
   * @param headerParams map of header of params to be used - can be null
   * @param queryParams multi map of query params keyed by string logically
   *          allows for K:"haircolor",V:"blond" AND K:"shoesize", V:"9","10"
   * @param pathParams vargs list of params to substitute in the path - note
   *          simply used in order
   * @return the type you asked for! or null
   * @throws RestClientException something went wrong making http call
   */
  public <T> T getResource(
      String path,
      Class<T> clazz,
      Map<String, String> headerParams,
      MultiValueMap<String, String> queryParams,
      Object... pathParams) throws RestClientException {
    log.debug("Enter getResources for path : {}", path);

    T responseObject = null;
    try {
      RetryCommand<ResponseEntity<String>> retryCommand = new RetryCommand<>(config.getRetryAttempts(),
          config.getRetryPauseMilliSeconds());
      UriComponents uriComponents = createUriComponents(path, queryParams, pathParams);
      HttpEntity<?> httpEntity = createHttpEntity(null, headerParams);
      ResponseEntity<String> response = retryCommand.run(
          () -> restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, httpEntity, String.class),
          shouldRetry());

      String responseBody = response.getBody();
      try {
        if (isError(response.getStatusCode())) {
          if (responseBody != null) {
            RestError error = objectMapper.readValue(responseBody, RestError.class);
            throw new RestClientException(String.format("%s [%s] %s", response.getStatusCode(),
                error.getError().getCode(), error.getError().getMessage()));
          } else {
            throw new RestClientException(response.getStatusCode().toString());
          }
        } else {
          responseObject = objectMapper.readValue(responseBody, clazz);
        }
      } catch (IOException e) {
        String msg = String.format("cause = %s - message = %s", e.getCause(), e.getMessage());
        log.error(msg);
        throw new RestClientException(msg);
      }
    } catch (CTPException e) {
      String msg = String.format("cause = %s - message = %s", e.getCause(), e.getMessage());
      log.error(msg);
      throw new RestClientException(msg);
    } finally {
    }

    return responseObject;
  }

  /**
   * Use to perform a GET that retrieves multiple instances of a resource
   *
   * @param <T> the type that will returned by the server we call
   * @param path the API path - can contain path params place holders in "{}" ie
   *          "/cases/{caseid}"
   * @param clazz the class type of the resource, a List<> of which is to be
   *          obtained
   * @param pathParams vargs list of params to substitute in the path - note
   *          simply used in order
   * @return a list of the type you asked for
   * @throws RestClientException something went wrong making http call
   */
  public <T> List<T> getResources(
      String path,
      Class<T[]> clazz,
      Object... pathParams)
      throws RestClientException {
    return getResources(path, clazz, null, null, pathParams);
  }

  /**
   * Use to perform a GET that retrieves multiple instances of a resource
   *
   * @param <T> the type that will returned by the server we call
   * @param path the API path - can contain path params place holders in "{}" ie
   *          "/cases/{caseid}"
   * @param clazz the array class type of the resource, a List<> of which is to
   *          be obtained
   * @param headerParams map of header of params to be used - can be null
   * @param queryParams multi map of query params keyed by string logically
   *          allows for K:"haircolor",V:"blond" AND K:"shoesize", V:"9","10"
   * @param pathParams vargs list of params to substitute in the path - note
   *          simply used in order
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

    List<T> responseList = new ArrayList<T>();
    try {
      RetryCommand<ResponseEntity<T[]>> retryCommand = new RetryCommand<>(config.getRetryAttempts(),
          config.getRetryPauseMilliSeconds());
      HttpEntity<?> httpEntity = createHttpEntity(null, headerParams);
      UriComponents uriComponents = createUriComponents(path, queryParams, pathParams);
      ResponseEntity<T[]> response = retryCommand
          .run(() -> restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, httpEntity, clazz), shouldRetry());

      if (!response.getStatusCode().is2xxSuccessful()) {
        log.error("Failed to get when calling {}", uriComponents.toUri());
        throw new RestClientException(String.format("Unexpected response status %s", response.getStatusCode().value()));
      }
      T[] responseArray = response.getBody();
      if (responseArray != null && responseArray.length > 0) {
        responseList = Arrays.asList(response.getBody());
      }
    } catch (CTPException e) {
      String msg = String.format("cause = %s - message = %s", e.getCause(), e.getMessage());
      log.error(msg);
      throw new RestClientException(msg);
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
  public <T, O> T postResource(
      String path,
      O objToPost,
      Class<T> clazz,
      Object... pathParams)
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
    return executePutOrPost(HttpMethod.POST, path, objToPost, clazz, headerParams, queryParams, pathParams);
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
  public <T, O> T putResource(
      String path,
      O objToPut,
      Class<T> clazz,
      Object... pathParams)
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
    return executePutOrPost(HttpMethod.PUT, path, objToPut, clazz, headerParams, queryParams, pathParams);
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
    log.debug("Enter getResources for path : {}", path);

    ResponseEntity<T> response = null;
    Map<String, String> headers;
    if (headerParams == null) {
      headers = new HashMap<String, String>();
    } else {
      headers = new HashMap<String, String>(headerParams);
    }
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    try {
      HttpEntity<O> httpEntity = createHttpEntity(objToPut, headers);
      UriComponents uriComponents = createUriComponents(path, queryParams, pathParams);
      RetryCommand<ResponseEntity<T>> retryCommand = new RetryCommand<>(config.getRetryAttempts(),
          config.getRetryPauseMilliSeconds());
      response = retryCommand
          .run(() -> restTemplate.exchange(uriComponents.toUri(), method, httpEntity, clazz), shouldRetry());

      if (!response.getStatusCode().is2xxSuccessful()) {
        log.error("Failed to put/post when calling {}", uriComponents.toUri());
        throw new RestClientException(String.format("Unexpected response status %s", response.getStatusCode().value()));
      }
    } catch (CTPException e) {
      String msg = String.format("cause = %s - message = %s", e.getCause(), e.getMessage());
      log.error(msg, e);
      throw new RestClientException(msg, e);
    }
    return response.getBody();
  }

  /**
   * used to create the URiComponents needed to call an endpoint
   *
   * @param path the API path - can contain path params place holders in "{}" ie
   *          "/cases/{caseid}"
   * @param queryParams multi map of query params keyed by string logically
   *          allows for K:"haircolor",V:"blond" AND K:"shoesize", V:"9","10"
   * @param pathParams vargs list of params to substitute in the path - note
   *          simply used in order
   * @return the components
   */
  protected UriComponents createUriComponents(String path,
      MultiValueMap<String, String> queryParams,
      Object... pathParams) {

    UriComponents uriComponentsWithOutQueryParams = UriComponentsBuilder.newInstance()
        .scheme(config.getScheme())
        .host(config.getHost())
        .port(config.getPort())
        .path(path)
        .buildAndExpand(pathParams);

    // Have to build UriComponents for query parameters separately as Expand
    // interprets braces in JSON query string values as URI template variables
    // to be replaced.
    UriComponents uriComponents = UriComponentsBuilder.newInstance()
        .uriComponents(uriComponentsWithOutQueryParams)
        .queryParams(queryParams)
        .build()
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
      byte[] encodedAuth = Base64.encode(
          auth.getBytes(Charset.forName("US-ASCII")));
      String authHeader = "Basic " + new String(encodedAuth);
      headers.set("Authorization", authHeader);
    }
    HttpEntity<H> httpEntity = new HttpEntity<H>(entity, headers);
    return httpEntity;
  }

}
