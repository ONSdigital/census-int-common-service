package uk.gov.ons.ctp.common.time;

import java.time.format.DateTimeFormatter;
import org.junit.Test;

public class DateTimeUtilTest {

  @Test
  public void testGetCurrentDateTimeInJsonFormat() {
    String currentDateTime = DateTimeUtil.getCurrentDateTimeInJsonFormat();

    String dateTimePattern = DateTimeUtil.DATE_FORMAT_IN_JSON;
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
    dateTimeFormatter.parse(currentDateTime);
  }
}
