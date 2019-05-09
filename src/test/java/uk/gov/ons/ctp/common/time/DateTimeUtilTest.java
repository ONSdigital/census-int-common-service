package uk.gov.ons.ctp.common.time;

import java.time.format.DateTimeFormatter;
import org.junit.Test;

public class DateTimeUtilTest {

  @Test
  public void testGetCurrentDateTimeUsesCorrectFormat() {
    // Invoke method under test, to get a string with the current datetime
    String currentDateTime = DateTimeUtil.getCurrentDateTimeInJsonFormat();

    // Verify formatting of datatime by parsing it
    String dateTimePattern = DateTimeUtil.DATE_FORMAT_IN_JSON;
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
    dateTimeFormatter.parse(currentDateTime);
  }
}
