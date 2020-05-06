package uk.gov.ons.ctp.common.domain;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.Test;

public class UniquePropertyReferenceNumberTest {
  private static final String UPRN_STR = "334111111111";
  private static final UniquePropertyReferenceNumber A_UPRN =
      new UniquePropertyReferenceNumber(UPRN_STR);
  private static final UniquePropertyReferenceNumber ANOTHER_UPRN =
      new UniquePropertyReferenceNumber("1347459999");

  @Data
  static class Dto {
    private UniquePropertyReferenceNumber uprn;
    private UniquePropertyReferenceNumber anotherUprn;
  }

  @Test
  public void shouldDeserialiseSingleValue() {
    final UniquePropertyReferenceNumber uprn =
        deserialise(UPRN_STR, UniquePropertyReferenceNumber.class);
    assertEquals(
        "resulting UPRN should match expected value: " + UPRN_STR,
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
}
