package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespondentRefusalDetails {

  private String type;
  private String report;
  private String agentId;
  private CollectionCaseCompact collectionCase = new CollectionCaseCompact();
  private Contact contact = new Contact();
  private AddressCompact address = new AddressCompact();
}
