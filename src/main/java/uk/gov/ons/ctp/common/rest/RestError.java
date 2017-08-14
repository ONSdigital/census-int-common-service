package uk.gov.ons.ctp.common.rest;

import lombok.Data;

/**
 * A Java type to deserialise REST error responses into
 */
@Data
public class RestError {
  private Error error;

  /**
   * The nested Error type
   */
  @Data
  public class Error {
    private String code;
    private String timestamp;
    private String message;
  }
}
