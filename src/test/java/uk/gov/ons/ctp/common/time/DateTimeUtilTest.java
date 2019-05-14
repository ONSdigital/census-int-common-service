package uk.gov.ons.ctp.common.time;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
  public void testConvertDateToLocalDateTime() {
    // Get hold of the current date and invoke method under test
    Date currentDate = new Date();
    LocalDateTime asLocalDateTime = DateTimeUtil.convertDateToLocalDateTime(currentDate);

    // Convert localDataTime back to a Date
    Date dateFollowingConversion =
        java.util.Date.from(asLocalDateTime.atZone(ZoneId.systemDefault()).toInstant());

    assertEquals(currentDate, dateFollowingConversion);
  }
}
