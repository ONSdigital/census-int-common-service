package uk.gov.ons.ctp.common.rest;

import java.io.IOException;

/**
 * A 'marker' exception to allow the RestClient to differentiate between ordinary IOExceptions and
 * those caused by a 401/UNAUTHORIZED error condition
 */
public class CTPIOException extends IOException {

  private static final long serialVersionUID = -2394166157547642459L;

  /**
   * CTPIOException constructor
   * @param msg message to include
   */
  public CTPIOException(String msg) {
    super(msg);
  }

  /**
   * Empty CTPIOException constructor
   */
  public CTPIOException() {
  }
}
