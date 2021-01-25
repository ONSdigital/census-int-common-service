package uk.gov.ons.ctp.common.rest;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

/**
 * A convenience class that wraps the Spring RestTemplate and eases its use around the typing,
 * headers, path and query params
 */
@Slf4j
public class RestClient {

  private RestClientConfig config;

  private RestTemplate restTemplate;

  private Map<HttpStatus, HttpStatus> httpErrorMapping;
  private HttpStatus httpDefaultStatus;

  private static Map<HttpStatus, HttpStatus> defaultBareBonesErrorMapping;
  private static final Logger logging = LoggerFactory.getLogger(RestClient.class);

  static {
    defaultBareBonesErrorMapping = new HashMap<HttpStatus, HttpStatus>();
    defaultBareBonesErrorMapping.put(HttpStatus.OK, HttpStatus.OK);
    defaultBareBonesErrorMapping.put(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND);
  }

  /**
   * Construct with no details of the server - will use the default RestClientConfig provides
   *
   * @throws CTPException
   */
  public RestClient() throws CTPException {
    this(new RestClientConfig());
  }

  /**
   * Constructor which uses no error code mappings.
   *
   * @param clientConfig contains data on how to connect to another service.
   * @throws CTPException
   */
  public RestClient(RestClientConfig clientConfig) throws CTPException {
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
   * @throws CTPException
   */
  public RestClient(
      RestClientConfig clientConfig,
      Map<HttpStatus, HttpStatus> httpErrorMapping,
      HttpStatus httpDefaultStatus)
      throws CTPException {
    this.config = clientConfig;
    this.httpErrorMapping = httpErrorMapping;
    this.httpDefaultStatus = httpDefaultStatus;
    init();
  }

  public void init() throws CTPException {
    PoolingHttpClientConnectionManager connectionManager = createConnectionManager();
    connectionManager.setDefaultMaxPerRoute(config.getConnectionManagerDefaultMaxPerRoute());
    connectionManager.setMaxTotal(config.getConnectionManagerMaxTotal());
    log.info(
        "Setting ConnectionManagerLimits for "
            + config.getHost()
            + " DefaultMaxPerRoute="
            + config.getConnectionManagerDefaultMaxPerRoute()
            + " MaxTotal="
            + config.getConnectionManagerMaxTotal());

    // Set http timeouts. Use '0' to disable and wait for an infinite amount of time
    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(config.getConnectTimeoutMillis())
            .setConnectionRequestTimeout(config.getConnectionRequestTimeoutMillis())
            .setSocketTimeout(config.getSocketTimeoutMillis())
            .build();

    HttpClient httpClient =
        HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .build();

    ClientHttpRequestFactory httpRequestFactory =
        new HttpComponentsClientHttpRequestFactory(httpClient);

    restTemplate = new RestTemplate(httpRequestFactory);
  }

  private PoolingHttpClientConnectionManager createConnectionManager() throws CTPException {
    try {
      SSLConnectionSocketFactory socketFactory =
          new SSLConnectionSocketFactory(
              SSLContext.getDefault(),
              new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"},
              null,
              SSLConnectionSocketFactory.getDefaultHostnameVerifier());
      Registry<ConnectionSocketFactory> registry =
          RegistryBuilder.<ConnectionSocketFactory>create()
              .register("http", PlainConnectionSocketFactory.INSTANCE)
              .register("https", socketFactory)
              .build();

      return new PoolingHttpClientConnectionManager(registry);
    } catch (NoSuchAlgorithmException e) {
      log.error("Failed to create SSL connection factory", e);
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
  }

  /**
   * Allow access to the underlying template
   *
   * @return the underlying template
   */
  RestTemplate getRestTemplate() {
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
   * @throws ResponseStatusException something went wrong making http call
   */
  public <T> T getResource(String path, Class<T> clazz, Object... pathParams)
      throws ResponseStatusException {
    return doHttpOperation(HttpMethod.GET, path, null, clazz, null, null, pathParams);
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
    return doHttpOperation(
        HttpMethod.GET, path, null, clazz, headerParams, queryParams, pathParams);
  }

  /**
   * Use to perform an http operation GET/PUT/POST to retrieve a single resource
   *
   * @param <T> the type that will returned by the server we call
   * @param <P> is the type of payload to send.
   * @param method is the type of http call to be made.
   * @param path the API path - can contain path params place holders in "{}" ie "/cases/{caseid}"
   * @param objToSend is the object to send as a Put or Post payload.
   * @param clazz the class type of the resource to be obtained
   * @param headerParams map of header of params to be used - can be null
   * @param queryParams multi map of query params keyed by string logically allows for
   *     K:"haircolor",V:"blond" AND K:"shoesize", V:"9","10"
   * @param pathParams vargs list of params to substitute in the path - note simply used in order
   * @return the type you asked for! or null
   * @throws ResponseStatusException something went wrong making http call. The reason field will
   *     contain the http response body.
   */
  private <T, P> T doHttpOperation(
      HttpMethod method,
      String path,
      P objToSend,
      Class<T> clazz,
      Map<String, String> headerParams,
      MultiValueMap<String, String> queryParams,
      Object... pathParams)
      throws ResponseStatusException {
    if (log.isDebugEnabled()) {
      log.debug("Enter doHttpOperation {} for path: {}", method.name(), path);
    }

    // Issue http request to other service
    HttpEntity<P> httpEntity = createHttpEntity(objToSend, headerParams);
    UriComponents uriComponents = createUriComponents(path, queryParams, pathParams);
    ResponseEntity<T> response;
    String errorMessage = "request failed for the given path";
    try {
      response = restTemplate.exchange(uriComponents.toUri(), method, httpEntity, clazz);
    } catch (HttpStatusCodeException e) {
      // Failure detected. For 4xx and 5xx status codes
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        errorMessage = "dealing with NOT_FOUND";
        logging
            .with("path", path)
            .with("methodName", method.name())
            .with("uriComponents", uriComponents)
            .with("Status", e.getStatusCode())
            .with("ResponseBody", e.getResponseBodyAsString())
            .warn(errorMessage);
      } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
        // Caller expected to handle this situation
        log.info("Too many requests response on {} for path: {}", method.name(), path);
      } else {
        logging
            .with("path", path)
            .with("methodName", method.name())
            .with("uriComponents", uriComponents)
            .with("statusCode", e.getStatusCode())
            .with("responseBody", e.getResponseBodyAsString())
            .error(errorMessage);
        if (log.isDebugEnabled()) {
          logging.debug(errorMessage, e);
        }
      }
      throw new ResponseStatusException(
          mapToExternalStatus(e.getStatusCode()), e.getResponseBodyAsString(), e);
    } catch (RestClientException e) {
      logging.with("path", path).with("methodName", method.name()).error(errorMessage, e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Internal processing error");
    }

    T responseObject = response.getBody();
    if (responseObject == null) {
      errorMessage = "Empty body returned for given path";
      logging
          .with("path", path)
          .with("methodName", method.name())
          .with("uriComponents", uriComponents)
          .with("statusCode", response.getStatusCode())
          .with("responseBody", response.getBody())
          .error(errorMessage);
      throw new ResponseStatusException(
          mapToExternalStatus(response.getStatusCode()), "Internal processing error. No response.");
    }

    if (log.isDebugEnabled()) {
      log.debug("Exit doHttpOperation {} for path: {}", method.name(), path);
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
   * @throws ResponseStatusException something went wrong making http call
   */
  public <T> List<T> getResources(String path, Class<T[]> clazz, Object... pathParams)
      throws ResponseStatusException {
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
   * @throws ResponseStatusException something went wrong making http call
   */
  public <T> List<T> getResources(
      String path,
      Class<T[]> clazz,
      Map<String, String> headerParams,
      MultiValueMap<String, String> queryParams,
      Object... pathParams)
      throws ResponseStatusException {

    if (log.isDebugEnabled()) {
      log.debug("Enter getResources for path : {}", path);
    }

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
   * @param <P> the type to be sent
   * @param path the url path
   * @param objToPost the object to be sent
   * @param clazz the expected response object type
   * @param pathParams var arg path params in {} placeholder order
   * @return the response object
   * @throws ResponseStatusException something went wrong calling the server
   */
  public <T, P> T postResource(String path, P objToPost, Class<T> clazz, Object... pathParams)
      throws ResponseStatusException {
    return postResource(path, objToPost, clazz, null, null, pathParams);
  }

  /**
   * used to post
   *
   * @param <T> the type that will returned by the server we call
   * @param <P> the type to be sent
   * @param path the url path
   * @param objToPost the object to be sent
   * @param clazz the expected response object type
   * @param headerParams map of header params
   * @param queryParams multi map of query params
   * @param pathParams var arg path params in {} placeholder order
   * @return the response object
   * @throws ResponseStatusException something went wrong calling the server
   */
  public <T, P> T postResource(
      String path,
      P objToPost,
      Class<T> clazz,
      Map<String, String> headerParams,
      MultiValueMap<String, String> queryParams,
      Object... pathParams)
      throws ResponseStatusException {
    return doHttpOperation(
        HttpMethod.POST, path, objToPost, clazz, headerParams, queryParams, pathParams);
  }

  /**
   * used to put
   *
   * @param <T> the type that will returned by the server we call
   * @param <P> the type to be sent
   * @param path the url path
   * @param objToPut the object to be sent
   * @param clazz the expected response object type
   * @param pathParams var arg path params in {} placeholder order
   * @return the response object
   * @throws ResponseStatusException something went wrong calling the server
   */
  public <T, P> T putResource(String path, P objToPut, Class<T> clazz, Object... pathParams)
      throws ResponseStatusException {
    return putResource(path, objToPut, clazz, null, null, pathParams);
  }

  /**
   * used to put
   *
   * @param <T> the type that will returned by the server we call
   * @param <P> the type to be sent
   * @param path the url path
   * @param objToPut the object to be sent
   * @param clazz the expected response object type
   * @param headerParams map of header params
   * @param queryParams multi map of query params
   * @param pathParams var arg path params in {} placeholder order
   * @return the response object
   * @throws ResponseStatusException something went wrong calling the server
   */
  public <T, P> T putResource(
      String path,
      P objToPut,
      Class<T> clazz,
      Map<String, String> headerParams,
      MultiValueMap<String, String> queryParams,
      Object... pathParams)
      throws ResponseStatusException {
    return doHttpOperation(
        HttpMethod.PUT, path, objToPut, clazz, headerParams, queryParams, pathParams);
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
