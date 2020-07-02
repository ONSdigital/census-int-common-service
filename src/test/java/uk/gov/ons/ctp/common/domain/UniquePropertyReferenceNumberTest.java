package uk.gov.ons.ctp.common.domain;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.Test;

public class UniquePropertyReferenceNumberTest {
  private static final String UPRN_MAX = "9999999999999";
  private static final String UPRN_MIN = "0";
  private static final String UPRN_MIN_FAIL = "-1";
  private static final String UPRN_MAX_FAIL = "99999999999991";
  private static final String UPRN_CONVERSION_FAIL = "x";
  private static final long UPRN_DEFAULT_VALUE = 0L;

  private static final UniquePropertyReferenceNumber A_UPRN =
      new UniquePropertyReferenceNumber(UPRN_MAX);
  private static final UniquePropertyReferenceNumber ANOTHER_UPRN =
      new UniquePropertyReferenceNumber(UPRN_MIN);

  @Data
  static class Dto {
    private UniquePropertyReferenceNumber uprn;
    private UniquePropertyReferenceNumber anotherUprn;
  }

  @Test
  public void shouldDeserialiseSingleValue() {
    final UniquePropertyReferenceNumber uprn =
        deserialise(UPRN_MAX, UniquePropertyReferenceNumber.class);
    assertEquals(
        "resulting UPRN should match expected value: " + UPRN_MAX,
        (Long) A_UPRN.getValue(),
        Long.valueOf(uprn.getValue()));
  }

  private ObjectMapper getObjectMapper() {
    return new ObjectMapper();
  }

  @SneakyThrows
  private <T> T deserialise(String json, Class<T> clazz) {
    return getObjectMapper().readValue(json, clazz);
  }

  @SneakyThrows
  private String prettySerialise(Object o) {
    return getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(o);
  }

  @Test
  public void shouldSerialiseAndDeserialise() {
    Dto dto = new Dto();
    dto.setUprn(A_UPRN);
    dto.setAnotherUprn(ANOTHER_UPRN);
    String json = prettySerialise(dto);
    Dto deser = deserialise(json, Dto.class);
    assertEquals(A_UPRN, deser.getUprn());
    assertEquals(ANOTHER_UPRN, deser.getAnotherUprn());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUprnMinFail() {
    new UniquePropertyReferenceNumber(UPRN_MIN_FAIL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUprnMaxFail() {
    new UniquePropertyReferenceNumber(UPRN_MAX_FAIL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUprnConversionFail() {
    new UniquePropertyReferenceNumber(UPRN_CONVERSION_FAIL);
  }

  @Test
  public void testUprnEmptyString() {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber("");
    assertEquals(UPRN_DEFAULT_VALUE, uprn.getValue());
  }

  @Test
  public void testUprnNull() {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(null);
    assertEquals(UPRN_DEFAULT_VALUE, uprn.getValue());
  }

  @Test
  public void testUprnWhiteSpace() {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber("  ");
    assertEquals(UPRN_DEFAULT_VALUE, uprn.getValue());
  }
}
