package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FulfilmentRequest {

  private String fulfilmentCode;
  private String caseId;
  private Address address = new Address();
  private Contact contact = new Contact();
}
