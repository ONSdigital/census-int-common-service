package uk.gov.ons.ctp.common.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A fake DTO to be used in the RestClientTest
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FakeDTO {
  private String hairColor;
  private Integer shoeSize;
}
