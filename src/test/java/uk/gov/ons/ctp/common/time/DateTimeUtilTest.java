package uk.gov.ons.ctp.common.time;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
  public void testFormatDate_winterTime() throws ParseException {
    Date testDateAsObject = createDate("2014-12-25T07:15:11.628Z");

    String testDateReconstituted = DateTimeUtil.formatDate(testDateAsObject);
    assertEquals("2014-12-25T07:15:11.628Z", testDateReconstituted);
  }

  @Test
  public void testFormatDate_summerTime() throws ParseException {
    Date testDateAsObject = createDate("2014-05-25T07:15:11.628Z");

    String testDateReconstituted = DateTimeUtil.formatDate(testDateAsObject);
    assertEquals("2014-05-25T08:15:11.628+01:00", testDateReconstituted);
  }

  @Test
  public void testFormatDate_winterWithZoneOffset() throws ParseException {
    Date testDateAsObject = createDate("2014-12-25T07:15:11.628+03:00");

    String testDateReconstituted = DateTimeUtil.formatDate(testDateAsObject);
    assertEquals("2014-12-25T04:15:11.628Z", testDateReconstituted);
  }

  @Test
  public void testFormatDate_summerWithZoneOffset() throws ParseException {
    Date testDateAsObject = createDate("2014-05-25T07:15:11.628+03:00");

    String testDateReconstituted = DateTimeUtil.formatDate(testDateAsObject);
    assertEquals("2014-05-25T05:15:11.628+01:00", testDateReconstituted);
  }

  private Date createDate(String testDateAsString) throws ParseException {
    // Convert test date from string to a Date object
    SimpleDateFormat dateTimeFormatter = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
    return dateTimeFormatter.parse(testDateAsString);
  }
}
