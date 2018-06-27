package uk.gov.ons.ctp.common.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * This class is intended to be used as the default means for parsing and formatting dates in RM
 * service requests/responses. It allows a number of different ISO8601 formats and aims to be
 * compatible with the Python ISO8601 format
 */
public class MultiIsoDateFormat extends AggregatedDateFormat {

  private static final DateFormat ISO_FORMAT_1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
  private static final DateFormat ISO_FORMAT_2 =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
  private static final DateFormat ISO_FORMAT_3 =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

  private static final DateFormat[] ALL_FORMATS = {ISO_FORMAT_1, ISO_FORMAT_2, ISO_FORMAT_3};

  /** Default constructor */
  public MultiIsoDateFormat() {
    super(ISO_FORMAT_3, ALL_FORMATS);
  }
}
