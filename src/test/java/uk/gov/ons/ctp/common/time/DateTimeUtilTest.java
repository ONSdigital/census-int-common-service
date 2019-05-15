package uk.gov.ons.ctp.common.time;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.junit.Test;

public class DateTimeUtilTest {

  @Test
  public void testGetCurrentDateTimeInJsonFormatUsesCorrectFormat() {
    // Invoke method under test, to get a string with the current datetime
    String currentDateTime = DateTimeUtil.getCurrentDateTimeInJsonFormat();

    // Verify formatting of datatime by parsing it
    String dateTimePattern = DateTimeUtil.DATE_FORMAT_IN_JSON;
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
    dateTimeFormatter.parse(currentDateTime);
  }

  @Test
  public void testFormatDate() throws ParseException {
    String testDateAsString = "2014-12-25T07:15:11.628Z";

    // Convert test date from string to a Date object
    SimpleDateFormat dateTimeFormatter = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
    Date testDateAsObject = dateTimeFormatter.parse(testDateAsString);

    // Invoke method under test and confirm that the completed 'String -> Date -> String' cycle
    // produces the same value that we started with
    String testDateReconstituted = DateTimeUtil.formatDate(testDateAsObject);
    assertEquals(testDateAsString, testDateReconstituted);
  }

  @Test
  public void testConvertDateToLocalDateTime_noTimeZoneOffset() throws ParseException {
    // Create test object
    String testDateAsString = "2014-12-25T07:15:11.628+0000";
    Date testDate = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON).parse(testDateAsString);

    // Invoke method under test. Convert to LocalDataTime
    LocalDateTime asLocalDateTime = DateTimeUtil.convertDateToLocalDateTime(testDate);

    // Format LocalDateTime as string and check
    String localDateAsString =
        asLocalDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
    assertEquals("2014-12-25T07:15:11.628", localDateAsString);
  }

  @Test
  public void testConvertDateToLocalDateTime_withTimeZoneOffset() throws ParseException {
    // Create test object
    String testDateAsString = "2014-12-25T07:15:11.628+0100";
    Date testDate = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON).parse(testDateAsString);

    // Invoke method under test. Convert to LocalDataTime
    LocalDateTime asLocalDateTime = DateTimeUtil.convertDateToLocalDateTime(testDate);

    // Format LocalDateTime as string and check
    String localDateAsString =
        asLocalDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
    assertEquals("2014-12-25T06:15:11.628", localDateAsString); // Note: 6am instead of 7am
  }
}
