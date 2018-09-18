package uk.gov.ons.ctp.common.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * This class is intended to be used as the default means for parsing and formatting dates in RM
 * service requests/responses. It allows a number of different ISO8601 formats and aims to be
 * compatible with the Python ISO8601 format
 */
public class MultiIsoDateFormat extends AggregatedDateFormat {

  private static final String ISO_FORMAT_1 = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
  private static final String ISO_FORMAT_2 = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";
  private static final String ISO_FORMAT_3 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

  /** Default constructor */
  public MultiIsoDateFormat() {
    DateFormat format1 = new SimpleDateFormat(ISO_FORMAT_1);
    DateFormat format2 = new SimpleDateFormat(ISO_FORMAT_2);
    DateFormat format3 = new SimpleDateFormat(ISO_FORMAT_3);
    DateFormat[] formats = {format1, format2, format3};


    init(format1, formats);
  }
}
