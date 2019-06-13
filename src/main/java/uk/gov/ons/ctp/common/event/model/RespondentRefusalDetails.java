package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespondentRefusalDetails implements EventPayload {

  private String type;
  private String report;
  private String agentId;
  private CollectionCaseCompact collectionCase;
  private Contact contact;
  private AddressCompact address;
}
