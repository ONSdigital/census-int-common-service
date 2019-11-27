package uk.gov.ons.ctp.common.model;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Test;
import uk.gov.ons.ctp.common.model.UniquePropertyReferenceNumber;

public class UPRNDeserializationTest {

  /**
   * Tests deserialization of the class
   *
   * @throws IOException - when mapper can't find the source, which it can here.
   */
  @Test
  public void deserializationTest() throws IOException {
    final Long expectedUPRN = 10013041069L;
    final ObjectMapper mapper = new ObjectMapper();
    final String jsonString = "{\"uprn\": " + expectedUPRN + "}";
    final UniquePropertyReferenceNumber uprn =
        mapper.readValue(jsonString, UniquePropertyReferenceNumber.class);
    assertEquals(
        "resulting UPRN should match expected value: " + expectedUPRN,
        expectedUPRN,
        Long.valueOf(uprn.getValue()));
  }
}
