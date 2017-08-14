package uk.gov.ons.ctp.common.rest;

import java.io.IOException;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Used to trap REST errors and handle them
 */
@Slf4j
public class RestClientErrorHandler implements ResponseErrorHandler {

  /**
   * Determinies if it really an error
   * @param response the response
   * @throws IOException if we cannot delve into the response
   */
  @Override
  public final boolean hasError(final ClientHttpResponse response) throws IOException {
    return RestClient.isError(response.getStatusCode());
  }

  /**
   * Actually deal with error - here we just log it and allow the RestClient to handle
   * the error - because RestClient needs to handle the deserialization of both the normal
   * response type OR the error type
   * @param response the response
   * @throws IOException if we cannot delve into the response
   */
  @Override
  public final void handleError(final ClientHttpResponse response) throws IOException {
    log.error("RestClient was sent http status code of {}", response.getStatusCode());
    switch (response.getStatusCode()) {
    case UNAUTHORIZED:
      throw new CTPIOException("Error in client request : " + response.getStatusText());
    case NOT_FOUND:
      break;
    default:
      throw new IOException("Error in client request : " + response.getStatusText());
    }
  }
}
