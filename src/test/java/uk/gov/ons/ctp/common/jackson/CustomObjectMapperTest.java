package uk.gov.ons.ctp.common.jackson;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import lombok.Data;
import org.junit.Test;

/** Test for common custom object mapper */
public class CustomObjectMapperTest {

  private static final long EPOCH_MS = 1532695775000L;
  private static final Instant EPOCH_INSTANT = Instant.ofEpochMilli(EPOCH_MS);
  private static final Date FRI_27_JULY_DATE = Date.from(EPOCH_INSTANT);
  private static final ZoneOffset ZONE_BST = ZoneOffset.ofHours(1);
  private static final OffsetDateTime FRI_27_JULY_OFFSET_DATE_TIME =
      OffsetDateTime.ofInstant(EPOCH_INSTANT, ZONE_BST);
  private static final String FRI_27_JULY_JSON_ISO_WITH_MS = "\"2018-07-27T12:49:35.000Z\"";
  private static final String FRI_27_JULY_JSON_ISO = "\"2018-07-27T13:49:35+01:00\"";

  static class SerializableObject {
    boolean ok = true;
  }

  @Data
  private static class ExampleClass {
    String name;

    @JsonSerialize(using = CustomDateSerialiser.class)
    Date sampleDate;
  }

  @Test
  public void testSerialisationOfClassWithDate() throws JsonProcessingException {
    final CustomObjectMapper objectMapper = new CustomObjectMapper();

    ExampleClass testObject = new ExampleClass();
    testObject.name = "Fred";
    testObject.sampleDate = FRI_27_JULY_DATE;

    String expected = "{\"name\":\"Fred\",\"sampleDate\":" + FRI_27_JULY_JSON_ISO_WITH_MS + "}";
    assertThat(objectMapper.writeValueAsString(testObject), is(expected));
  }

  @Test
  public void testSerialisationWithoutMilliseconds() throws JsonProcessingException {
    final CustomObjectMapper objectMapper = new CustomObjectMapper();

    Date testDate = objectMapper.convertValue("2018-07-27T12:49:35Z", Date.class);

    assertThat(objectMapper.writeValueAsString(testDate), is("\"2018-07-27T12:49:35.000Z\""));
  }

  @Test
  public void testSerialisationWithoutSeconds() throws JsonProcessingException {
    final CustomObjectMapper objectMapper = new CustomObjectMapper();

    Date testDate = objectMapper.convertValue("2018-07-27T12:49Z", Date.class);

    assertThat(objectMapper.writeValueAsString(testDate), is("\"2018-07-27T12:49:00.000Z\""));
  }

  @Test
  public void testMapperIsConfiguredToWriteIsoDatesFromDate() throws JsonProcessingException {
    final CustomObjectMapper objectMapper = new CustomObjectMapper();

    assertThat(objectMapper.writeValueAsString(FRI_27_JULY_DATE), is(FRI_27_JULY_JSON_ISO_WITH_MS));
  }

  @Test
  public void testMapperIsConfiguredToReadIsoDatesToDate() throws IOException {
    final CustomObjectMapper objectMapper = new CustomObjectMapper();

    assertThat(
        objectMapper.readValue(FRI_27_JULY_JSON_ISO_WITH_MS, Date.class), is(FRI_27_JULY_DATE));
  }

  @Test
  public void testMapperIsConfiguredToWriteIsoDatesFromOffsetDateTime()
      throws JsonProcessingException {
    final CustomObjectMapper objectMapper = new CustomObjectMapper();

    assertThat(
        objectMapper.writeValueAsString(FRI_27_JULY_OFFSET_DATE_TIME), is(FRI_27_JULY_JSON_ISO));
  }

  @Test
  public void testMapperIsConfiguredToReadIsoDatesToOffsetDateTime() throws IOException {
    final CustomObjectMapper objectMapper = new CustomObjectMapper();

    OffsetDateTime offsetDateTime =
        objectMapper.readValue(FRI_27_JULY_JSON_ISO, OffsetDateTime.class);

    long actualEpochMs = offsetDateTime.toInstant().toEpochMilli();
    long expectedEpochMs = FRI_27_JULY_OFFSET_DATE_TIME.toInstant().toEpochMilli();

    assertThat(actualEpochMs, is(expectedEpochMs));
  }

  @Test
  public void testMapperIsConfiguredToSkipUnknownProperties() throws IOException {
    final CustomObjectMapper objectMapper = new CustomObjectMapper();

    assertThat(
        objectMapper.readValue("{ \"ok\": true, \"skipped\": true }", SerializableObject.class),
        is(instanceOf(SerializableObject.class)));
  }
}
