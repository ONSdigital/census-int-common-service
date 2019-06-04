package uk.gov.ons.ctp.common.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespondentAuthenticatedEvent extends GenericEvent {

  private RespondentAuthenticatedPayload payload = new RespondentAuthenticatedPayload();
}
